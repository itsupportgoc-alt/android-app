/** Document page composable displaying the formatted text content. */
@Composable
fun DocumentPage(
    modifier: Modifier = Modifier,
    showSpellingErrors: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val (pageNumber, totalPages) = 1 to 2

    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Horizontal ruler
        Box(
            modifier = Modifier
                .height(1.dp)
                .background(colors.divider),
        )

        // Page content
        Box(
            modifier = Modifier
                .width(DeviceScreenWidth.ReferenceWidth.In420pt)
                .padding(horizontal = 16.dp)
                .background(
                    Color(
                        light = Color(0xFFFDFDFD),
                        dark = Color(0xFF212121),
                    )
                ),
        ) {
            // Header line
            Row(
                modifier = Modifier
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "The Open Studio Handbook",
                    style = MaterialTheme.typography overline,
                    color = colors.onSurface.copyWith(alpha = 0.6f),
                )

                Text(
                    text = "Spring edition",
                    style = MaterialTheme.typography overline,
                    color = colors.onSurface.copyWith(alpha = 0.6f),
                    textAlign = TextAlign.End,
                )

                // Thin rule beneath header
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .background(colors.divider)
                        .fillMaxWidth(0.3f),
                )
            }

            // Main title
            Text(
                text = "The Open Studio Handbook",
                style = MaterialTheme.typography.displayLarge,
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 2.dp),
            )

            // Subtitle
            Text(
                text = "How a small community of makers shares space, tools and time",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface.copyWith(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Body Paragraph 1
            Spacer(modifier = Modifier.height(8.dp))
            Spans(
                text = "Every Thursday evening the old print shop on Alder Street fills with people who make things. ",
                spans = listOf(
                    SpanRun(
                        text = "Painters",
                        style = SpanStyle.Bold,
                    ),
                    SpanRun(
                        text = "letterpress regulars",
                        style = SpanStyle.Italic,
                    ),
                ),
            )
            // underline "pleasant" with red wavy
            if (showSpellingErrors) {
                Text(
                    text = "pleasant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF0000), // red
                    // TODO: add underline span
                )
            }
            Text(
                text = ". This handbook collects what we have learned about running a shared studio: the routines that keep it pleasant, the tools we pool, and the small agreements that let thirty strangers become a crew.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // H2 heading
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Why share a studio?",
                style = MaterialTheme.typography.h5,
                color = colors.primary,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            // Body Paragraph 2
            Text(
                text = "Rent is the obvious reason, but it is not the best one. A shared room puts your work next to work you would never have seen otherwise. A ceramicist explains glaze chemistry to a comic artist; a photographer borrows a weaver's loom for a still life. The cross-talk is the point.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Pull quote
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(DeviceScreenWidth.ReferenceWidth.In420pt)
                    .padding(horizontal = 16.dp)
                    .background(colors.surface)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\"A studio is a place where unfinished things are welcome.\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface.copyWith(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                )
            }

            // H3 heading
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What we agree on",
                style = MaterialTheme.typography.h5,
                color = colors.primary,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            // Bulleted list
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                bulletItem("Clean as you go.", "Leave a table the way you would like to find it.")
                bulletItem("Label what is yours.", "Unlabelled supplies on the commons shelf are fair game.")
                bulletItem("Ask before you borrow.", "Even from the shelf, a quick note helps everyone keep track.")
            }
        }
    }
}

/** Bullet item with bold lead-in. */
private fun bulletItem(
    lead: String,
    explanation: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(
            crossAxisAlignment = CrossAxisAlignment.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = lead,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface.copyWith(alpha = 0.6f),
            )
        }
    }
}

/** Span run for text styling. */
private data class SpanRun(
    val text: String,
    val style: SpanStyle,
)

/** Span style enum. */
private enum class SpanStyle {
    Bold,
    Italic,
    Underline,
}

/** Spans composable. */
@Composable
fun Spans(
    text: String,
    spans: List<SpanRun>,
    modifier: Modifier = Modifier,
) {
    var rememberedSpans by remember { mutableStateOf(spans) }

    androidx.compose.material3.Spans(
        modifier = modifier,
        strings = text,
        styleRuns = rememberedSpans,
    ) {
        // Apply styles to individual characters/words
        // This is a simplified version - full implementation would need
        // proper Spans API usage
    }
}