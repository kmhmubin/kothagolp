package com.kmhmubin.kothagolp.epub

import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml

/**
 * Builds EPUB XML content files using kotlin-xml-builder
 */
object EpubBuilder {

    private val printOptions = PrintOptions(
        pretty = true,
        singleLineTextElements = true,
        useSelfClosingTags = true
    )

    // ================================================================
    // CONTAINER.XML - Points to content.opf
    // ================================================================

    fun buildContainerXml(): String {
        return xml("container") {
            xmlns = "urn:oasis:names:tc:opendocument:xmlns:container"
            attribute("version", "1.0")

            "rootfiles" {
                "rootfile" {
                    attribute("full-path", "OEBPS/content.opf")
                    attribute("media-type", "application/oebps-package+xml")
                }
            }
        }.toString(printOptions)
    }

    // ================================================================
    // CONTENT.OPF - Package document (metadata, manifest, spine)
    // ================================================================

    fun buildContentOpf(
        metadata: EpubMetadata,
        chapters: List<EpubChapter>,
        hasCover: Boolean
    ): String {
        return xml("package") {
            xmlns = "http://www.idpf.org/2007/opf"
            attribute("version", "3.0")
            attribute("unique-identifier", "BookId")

            // Metadata section
            "metadata" {
                xmlns = "http://www.idpf.org/2007/opf"
                attribute("xmlns:dc", "http://purl.org/dc/elements/1.1/")
                attribute("xmlns:opf", "http://www.idpf.org/2007/opf")

                "dc:identifier" {
                    attribute("id", "BookId")
                    -metadata.identifier
                }
                "dc:title" { -metadata.title }
                "dc:language" { -metadata.language }
                "dc:creator" { -(metadata.author ?: "Unknown") }
                "dc:publisher" { -metadata.publisher }
                "dc:date" { -metadata.creationDate }

                metadata.description?.let { desc ->
                    "dc:description" { -desc.take(2000) }
                }

                metadata.tags.forEach { tag ->
                    "dc:subject" { -tag }
                }

                // EPUB 3 meta for modified date
                "meta" {
                    attribute("property", "dcterms:modified")
                    -java.time.ZonedDateTime.now().format(
                        java.time.format.DateTimeFormatter.ISO_INSTANT
                    ).replace("Z", "+00:00")
                }

                if (hasCover) {
                    "meta" {
                        attribute("name", "cover")
                        attribute("content", "cover-image")
                    }
                }
            }

            // Manifest section
            "manifest" {
                // Nav document (EPUB 3)
                "item" {
                    attribute("id", "nav")
                    attribute("href", "nav.xhtml")
                    attribute("media-type", "application/xhtml+xml")
                    attribute("properties", "nav")
                }

                // NCX (EPUB 2 compatibility)
                "item" {
                    attribute("id", "ncx")
                    attribute("href", "toc.ncx")
                    attribute("media-type", "application/x-dtbncx+xml")
                }

                // Stylesheet
                "item" {
                    attribute("id", "stylesheet")
                    attribute("href", "styles.css")
                    attribute("media-type", "text/css")
                }

                // Cover image
                if (hasCover) {
                    "item" {
                        attribute("id", "cover-image")
                        attribute("href", "cover.jpg")
                        attribute("media-type", "image/jpeg")
                        attribute("properties", "cover-image")
                    }

                    "item" {
                        attribute("id", "cover")
                        attribute("href", "cover.xhtml")
                        attribute("media-type", "application/xhtml+xml")
                    }
                }

                // Title page
                "item" {
                    attribute("id", "titlepage")
                    attribute("href", "title.xhtml")
                    attribute("media-type", "application/xhtml+xml")
                }

                // Chapters
                chapters.forEach { chapter ->
                    "item" {
                        attribute("id", chapter.id)
                        attribute("href", "chapters/${chapter.fileName}")
                        attribute("media-type", "application/xhtml+xml")
                    }
                }
            }

            // Spine section (reading order)
            "spine" {
                attribute("toc", "ncx")

                if (hasCover) {
                    "itemref" {
                        attribute("idref", "cover")
                        attribute("linear", "no")
                    }
                }

                "itemref" { attribute("idref", "titlepage") }

                chapters.forEach { chapter ->
                    "itemref" { attribute("idref", chapter.id) }
                }
            }

            // Guide section
            "guide" {
                if (hasCover) {
                    "reference" {
                        attribute("type", "cover")
                        attribute("title", "Cover")
                        attribute("href", "cover.xhtml")
                    }
                }
                "reference" {
                    attribute("type", "title-page")
                    attribute("title", "Title Page")
                    attribute("href", "title.xhtml")
                }
                if (chapters.isNotEmpty()) {
                    "reference" {
                        attribute("type", "text")
                        attribute("title", "Start")
                        attribute("href", "chapters/${chapters.first().fileName}")
                    }
                }
            }
        }.toString(printOptions)
    }

    // ================================================================
    // TOC.NCX - Navigation Control (EPUB 2 compatibility)
    // ================================================================

    fun buildTocNcx(metadata: EpubMetadata, chapters: List<EpubChapter>): String {
        return xml("ncx") {
            xmlns = "http://www.daisy.org/z3986/2005/ncx/"
            attribute("version", "2005-1")

            "head" {
                "meta" {
                    attribute("name", "dtb:uid")
                    attribute("content", metadata.identifier)
                }
                "meta" {
                    attribute("name", "dtb:depth")
                    attribute("content", "1")
                }
                "meta" {
                    attribute("name", "dtb:totalPageCount")
                    attribute("content", "0")
                }
                "meta" {
                    attribute("name", "dtb:maxPageNumber")
                    attribute("content", "0")
                }
            }

            "docTitle" {
                "text" { -metadata.title }
            }

            "navMap" {
                // Title page
                "navPoint" {
                    attribute("id", "titlepage")
                    attribute("playOrder", "1")
                    "navLabel" { "text" { -"Title Page" } }
                    "content" { attribute("src", "title.xhtml") }
                }

                // Chapters
                chapters.forEachIndexed { index, chapter ->
                    "navPoint" {
                        attribute("id", chapter.id)
                        attribute("playOrder", "${index + 2}")
                        "navLabel" { "text" { -chapter.title } }
                        "content" {
                            attribute("src", "chapters/${chapter.fileName}")
                        }
                    }
                }
            }
        }.toString(printOptions)
    }

    // ================================================================
    // NAV.XHTML - EPUB 3 Navigation Document
    // ================================================================

    fun buildNavXhtml(metadata: EpubMetadata, chapters: List<EpubChapter>): String {
        return buildXhtmlDocument(
            title = "Table of Contents",
            bodyContent = buildString {
                appendLine("""<nav epub:type="toc" id="toc">""")
                appendLine("<h1>Table of Contents</h1>")
                appendLine("<ol>")
                appendLine("""<li><a href="title.xhtml">Title Page</a></li>""")
                chapters.forEach { chapter ->
                    val escapedTitle = escapeXmlAttribute(chapter.title)
                    appendLine("""<li><a href="chapters/${chapter.fileName}">$escapedTitle</a></li>""")
                }
                appendLine("</ol>")
                appendLine("</nav>")
            },
            additionalAttributes = """xmlns:epub="http://www.idpf.org/2007/ops""""
        )
    }

    // ================================================================
    // STYLES.CSS - Stylesheet
    // ================================================================

    fun buildStylesCss(): String = """
    /* Kothagolp EPUB Stylesheet */
    
    @page {
        margin: 1em;
    }
    
    body {
        font-family: Georgia, "Times New Roman", serif;
        font-size: 1em;
        line-height: 1.6;
        margin: 0;
        padding: 1em;
        text-align: justify;
        -webkit-hyphens: auto;
        hyphens: auto;
    }
    
    h1, h2, h3 {
        font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
        text-align: left;
        margin-top: 1.5em;
        margin-bottom: 0.5em;
        line-height: 1.2;
    }
    
    h1 {
        font-size: 1.8em;
        border-bottom: 1px solid #ccc;
        padding-bottom: 0.3em;
    }
    
    h2 {
        font-size: 1.4em;
    }
    
    p {
        margin: 0 0 1em 0;
        text-indent: 1.5em;
    }
    
    p:first-of-type {
        text-indent: 0;
    }
    
    /* Inline formatting */
    strong, b {
        font-weight: bold;
    }
    
    em, i {
        font-style: italic;
    }
    
    u {
        text-decoration: underline;
    }
    
    s, strike, del {
        text-decoration: line-through;
    }
    
    code, kbd {
        font-family: "Courier New", Courier, monospace;
        font-size: 0.9em;
        background-color: #f4f4f4;
        padding: 0.1em 0.3em;
        border-radius: 3px;
    }
    
    small {
        font-size: 0.85em;
    }
    
    sub {
        font-size: 0.75em;
        vertical-align: sub;
    }
    
    sup {
        font-size: 0.75em;
        vertical-align: super;
    }
    
    mark {
        background-color: #ffff00;
        padding: 0.1em;
    }
    
    q {
        font-style: italic;
    }
    
    q::before {
        content: ""\";
}

q::after {
    content: ""\";
    }

    abbr {
        text-decoration: underline dotted;
    }

    cite {
        font-style: italic;
    }

    /* Cover page */
    .cover {
        text-align: center;
        padding: 0;
        margin: 0;
    }

    .cover img {
        max-width: 100%;
        max-height: 100vh;
        object-fit: contain;
    }

    /* Title page */
    .title-page {
        text-align: center;
        padding-top: 20%;
    }

    .title-page h1 {
        font-size: 2em;
        border: none;
        margin-bottom: 0.5em;
    }

    .title-page .author {
        font-size: 1.3em;
        color: #555;
        margin-bottom: 2em;
    }

    .title-page .meta {
        font-size: 0.9em;
        color: #777;
        margin-top: 3em;
    }

    .title-page .description {
        text-align: justify;
        font-style: italic;
        margin: 2em auto;
        max-width: 80%;
        padding: 1em;
        border-left: 3px solid #ccc;
    }

    .title-page .tags {
        margin-top: 1em;
    }

    .title-page .tag {
        display: inline-block;
        background: #f0f0f0;
        padding: 0.2em 0.6em;
        margin: 0.2em;
        border-radius: 3px;
        font-size: 0.8em;
    }

    /* Chapter styling */
    .chapter-title {
        margin-bottom: 2em;
    }

    .chapter-content p {
        margin-bottom: 1em;
    }

    /* Special formatting for novel content */
    .chapter-content strong {
        font-weight: bold;
        color: inherit;
    }

    .chapter-content em {
        font-style: italic;
    }

    /* Horizontal rule styling */
    hr {
        border: none;
        border-top: 1px solid #ccc;
        margin: 2em 0;
    }
""".trimIndent()

    // ================================================================
    // COVER.XHTML - Cover page
    // ================================================================

    fun buildCoverXhtml(metadata: EpubMetadata): String {
        return buildXhtmlDocument(
            title = "Cover",
            bodyContent = """
                <div class="cover">
                    <img src="cover.jpg" alt="Cover for ${escapeXmlAttribute(metadata.title)}" />
                </div>
            """.trimIndent()
        )
    }

    // ================================================================
    // TITLE.XHTML - Title page
    // ================================================================

    fun buildTitleXhtml(metadata: EpubMetadata, chapterCount: Int): String {
        val tagsHtml = if (metadata.tags.isNotEmpty()) {
            """
            <div class="tags">
                ${metadata.tags.take(10).joinToString("") {
                """<span class="tag">${escapeXmlAttribute(it)}</span>"""
            }}
            </div>
            """.trimIndent()
        } else ""

        val descriptionHtml = metadata.description?.let { desc ->
            """<div class="description">${escapeXmlAttribute(desc.take(500))}</div>"""
        } ?: ""

        return buildXhtmlDocument(
            title = metadata.title,
            bodyContent = """
                <div class="title-page">
                    <h1>${escapeXmlAttribute(metadata.title)}</h1>
                    <p class="author">by ${escapeXmlAttribute(metadata.author ?: "Unknown")}</p>
                    $descriptionHtml
                    $tagsHtml
                    <div class="meta">
                        <p>$chapterCount Chapters</p>
                        <p>Generated by Kothagolp</p>
                        <p>${metadata.creationDate}</p>
                    </div>
                </div>
            """.trimIndent()
        )
    }

    // ================================================================
    // CHAPTER XHTML
    // ================================================================

    fun buildChapterXhtml(chapter: EpubChapter): String {
        return buildXhtmlDocument(
            title = chapter.title,
            bodyContent = """
                <h1 class="chapter-title">${escapeXmlAttribute(chapter.title)}</h1>
                <div class="chapter-content">
                    ${chapter.toXhtml()}
                </div>
            """.trimIndent()
        )
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun buildXhtmlDocument(
        title: String,
        bodyContent: String,
        additionalAttributes: String = ""
    ): String {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml" $additionalAttributes>
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>${escapeXmlAttribute(title)}</title>
                <link rel="stylesheet" type="text/css" href="styles.css" />
            </head>
            <body>
                $bodyContent
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeXmlAttribute(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}