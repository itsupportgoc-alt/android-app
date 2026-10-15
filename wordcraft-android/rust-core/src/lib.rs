//! JNI bridge between the Android app and the WordCraft document engine.
//!
//! The upstream `wordcraft-*` crates are used unmodified; this crate only exposes an opaque
//! session handle, JSON state, command dispatch and byte-based file I/O over the C ABI.
//!
//! All string arguments and results cross the boundary as **UTF-8 byte arrays** — JNI's
//! `String` uses modified UTF-8 (CESU-8 for supplementary characters), which would corrupt
//! emoji and other non-BMP text; byte arrays carry exact UTF-8.
//!
//! Threading: the app uses one session at a time from its engine dispatcher thread; sessions are
//! additionally guarded by a global registry so no call can dangle.

pub mod vm;

use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jlong};
use jni::JNIEnv;
use serde_json::{json, Value};
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{LazyLock, Mutex, MutexGuard};
use wordcraft_doc::Document;
use wordcraft_engine::Session;

static NEXT_ID: AtomicU64 = AtomicU64::new(1);
static SESSIONS: LazyLock<Mutex<std::collections::HashMap<u64, Session>>> = LazyLock::new(|| Mutex::new(std::collections::HashMap::new()));
static LAST_ERR: LazyLock<Mutex<String>> = LazyLock::new(|| Mutex::new(String::new()));

fn sessions() -> MutexGuard<'static, std::collections::HashMap<u64, Session>> {
    SESSIONS.lock().unwrap_or_else(|e| e.into_inner())
}

fn set_err(msg: impl Into<String>) {
    if let Ok(mut e) = LAST_ERR.lock() {
        *e = msg.into();
    }
}

fn err_json(msg: impl std::fmt::Display) -> String {
    json!({ "error": msg.to_string() }).to_string()
}

// ---------------------------------------------------------------- session registry (testable)

#[doc(hidden)]
pub fn open_session(name: &str, bytes: &[u8]) -> Result<Session, String> {
    wordcraft_engine::io::open_bytes(name, bytes).map(Session::new)
}

#[doc(hidden)]
pub fn save_session(s: &Session, name: &str) -> Result<Vec<u8>, String> {
    wordcraft_engine::io::save_bytes(name, &s.doc)
}

#[doc(hidden)]
pub fn store_session(s: Session) -> u64 {
    let id = NEXT_ID.fetch_add(1, Ordering::Relaxed);
    sessions().insert(id, s);
    id
}

#[doc(hidden)]
pub fn take_session(id: u64) -> Option<Session> {
    sessions().remove(&id)
}

// ---------------------------------------------------------------- shared helpers

fn with_session<T>(id: jlong, f: impl FnOnce(&mut Session) -> T, on_missing: T) -> T {
    let mut map = sessions();
    match map.get_mut(&(id as u64)) {
        Some(s) => f(s),
        None => on_missing,
    }
}

/// UTF-8 bytes from Kotlin → `String` (lossy only for genuinely invalid input).
fn arg_string(env: &JNIEnv, b: &JByteArray) -> Option<String> {
    match env.convert_byte_array(b) {
        Ok(bytes) => Some(String::from_utf8_lossy(&bytes).into_owned()),
        Err(e) => {
            set_err(format!("byte argument: {e}"));
            None
        }
    }
}

fn arg_bytes(env: &JNIEnv, b: &JByteArray) -> Option<Vec<u8>> {
    match env.convert_byte_array(b) {
        Ok(v) => Some(v),
        Err(e) => {
            set_err(format!("byte argument: {e}"));
            None
        }
    }
}

fn ret_string(env: JNIEnv, s: Option<String>) -> jbyteArray {
    ret_bytes(env, s.map(|s| s.into_bytes()))
}

fn ret_bytes(env: JNIEnv, b: Option<Vec<u8>>) -> jbyteArray {
    match b {
        Some(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(jb) => jb.as_raw(),
            Err(e) => {
                set_err(format!("byte_array: {e}"));
                std::ptr::null_mut()
            }
        },
        None => std::ptr::null_mut(),
    }
}

/// Guard a body against panics: the never-crash rule applies across the FFI boundary too.
fn guard<T: Default>(label: &str, f: impl FnOnce() -> T) -> T {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(f)) {
        Ok(v) => v,
        Err(_) => {
            set_err(format!("panic in {label}"));
            T::default()
        }
    }
}

// ---------------------------------------------------------------- JNI entry points

/// `sessionNew(): Long` — new blank document.
#[unsafe(no_mangle)]
pub extern "C" fn Java_com_wordcraft_documentcore_WordCraftNativeKt_nativeSessionNew(_env: JNIEnv, _cls: JClass) -> jlong {
    guard("sessionNew", || store_session(Session::new(Document::new())) as jlong)
}

/// `openBytes(nameUtf8: ByteArray, data: ByteArray): Long` — 0 on failure (see `lastError`).
#[unsafe(no_mangle)]
pub extern "C" fn Java_com_wordcraft_documentcore_WordCraftNativeKt_nativeOpenBytes(env: JNIEnv, _cls: JClass, name: JByteArray, data: JByteArray) -> jlong {
    guard("openBytes", || {
        let Some(name) = arg_string(&env, &name) else { return 0 };
        let Some(bytes) = arg_bytes(&env, &data) else { return 0 };
        match open_session(&name, &bytes) {
            Ok(s) => store_session(s) as jlong,
            Err(e) => {
                set_err(e);
                0
            }
        }
    })
}

/// `closeSession(handle: Long)`.
#[unsafe(no_mangle)]
pub extern "C" fn Java_com_wordcraft_documentcore_WordCraftNativeKt_nativeCloseSession(_env: JNIEnv, _cls: JClass, handle: jlong) {
    let _ = take_session(handle as u64);
}

/// `saveBytes(handle: Long, nameUtf8: ByteArray): ByteArray?` — null on error (see `lastError`).
#[unsafe(no_mangle)]
pub extern "C" fn Java_com_wordcraft_documentcore_WordCraftNativeKt_nativeSaveBytes(env: JNIEnv, _cls: JClass, handle: jlong, name: JByteArray) -> jbyteArray {
    let out = guard("saveBytes", || {
        let Some(name) = arg_string(&env, &name) else { return None };
        with_session(
            handle,
            |s| match save_session(s, &name) {
                Ok(b) => Some(b),
                Err(e) => {
                    set_err(e);
                    None
                }
            },
            None,
        )
    });
    ret_bytes(env, out)
}

/// `runCommand(handle, idUtf8, paramsUtf8): ByteArray` — UTF-8 JSON `{"ok": ...}` / `{"error": ...}`.
#[unsafe(no_mangle)]
pub extern "C" fn Java_com_wordcraft_documentcore_WordCraftNativeKt_nativeRunCommand(env: JNIEnv, _cls: JClass, handle: jlong, id: JByteArray, params: JByteArray) -> jbyteArray {
    let out = guard("runCommand", || {
        let Some(id) = arg_string(&env, &id) else { return Some(err_json("bad id")) };
        let Some(params) = arg_string(&env, &params) else { return Some(err_json("bad params")) };
        let params: Value = if params.trim().is_empty() {
            json!({})
        } else {
            match serde_json::from_str(&params) {
                Ok(v) => v,
                Err(e) => return Some(err_json(format!("params JSON: {e}"))),
            }
        };
        with_session(
            handle,
            |s| match s.run(&id, &params) {
                Ok(v) => Some(json!({ "ok": v }).to_string()),
                Err(e) => Some(err_json(e)),
            },
            Some(err_json("unknown session")),
        )
    });
    ret_string(env, out)
}

/// `state(handle: Long): ByteArray?` — the editor view model (see [`vm::state_json`]), UTF-8 JSON.
#[unsafe(no_mangle)]
pub extern "C" fn Java_com_wordcraft_documentcore_WordCraftNativeKt_nativeState(env: JNIEnv, _cls: JClass, handle: jlong) -> jbyteArray {
    let out = guard("state", || with_session(handle, |s| Some(vm::state_json(s).to_string()), None));
    ret_string(env, out)
}

/// `lastError(): ByteArray?` — the last failure message (open/save), UTF-8.
#[unsafe(no_mangle)]
pub extern "C" fn Java_com_wordcraft_documentcore_WordCraftNativeKt_nativeLastError(env: JNIEnv, _cls: JClass) -> jbyteArray {
    let out = LAST_ERR.lock().map(|e| e.clone()).ok();
    ret_string(env, out)
}
