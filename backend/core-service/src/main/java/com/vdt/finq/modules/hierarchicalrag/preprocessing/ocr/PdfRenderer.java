package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import lombok.extern.slf4j.Slf4j;

/**
 * Renders one PDF page into a grayscale JPEG, ready to be sent to the vision model.
 *
 * Not a Spring bean: one instance per document, opened and closed around the job.
 * Use it with try-with-resources so the PDF is always closed.
 *
 * <pre>
 * try (PdfRenderer renderer = PdfRenderer.open(pdfPath)) {
 *     byte[] jpeg = renderer.render(5, 170, 0.85f);   // page 6 on paper
 * }
 * </pre>
 */
@Slf4j
public final class PdfRenderer implements AutoCloseable {

    /** JPEG is the only format we produce - the vision model reads it fine and it is small. */
    private static final String JPEG_FORMAT = "jpeg";

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final Path source;

    private PdfRenderer(PDDocument document, Path source) {
        this.document = document;
        this.source = source;
        // Built once and reused for every page - rebuilding it per page is wasted work.
        this.renderer = new PDFRenderer(document);
    }

    /** Opens a PDF. The caller owns the returned object and must close it. */
    public static PdfRenderer open(Path pdfPath) throws IOException {
        if (!Files.isRegularFile(pdfPath)) {
            throw new IOException("PDF not found: " + pdfPath);
        }
        // PDFBox 3 loads through Loader. Older guides say PDDocument.load(...) - that is PDFBox 2.
        PDDocument document = Loader.loadPDF(pdfPath.toFile());
        log.debug("opened {} ({} pages)", pdfPath.getFileName(), document.getNumberOfPages());
        return new PdfRenderer(document, pdfPath);
    }

    /** Number of pages in the PDF. */
    public int pageCount() {
        return document.getNumberOfPages();
    }

    /**
     * Renders one page to a grayscale JPEG.
     *
     * @param pageIndex   zero-based, so page 6 on paper is index 5
     * @param dpi         170 by default; the retry ladder lowers it to 130 then 110
     * @param jpegQuality 0.0 to 1.0, we use 0.85
     */
    public byte[] render(int pageIndex, int dpi, float jpegQuality) throws IOException {
        if (pageIndex < 0 || pageIndex >= pageCount()) {
            // Checked here so the message says which page and which file.
            // PDFBox would throw something much harder to read.
            throw new IllegalArgumentException(
                    "page index %d out of range 0..%d in %s"
                            .formatted(pageIndex, pageCount() - 1, source.getFileName()));
        }

        BufferedImage image;
        // PDDocument is NOT thread safe. Six worker threads rendering at once corrupt it,
        // and the failure is intermittent - it may only show up after a hundred pages.
        // Rendering takes ~200ms while the network call takes 20-120s, so serialising
        // this part costs almost nothing.
        synchronized (document) {
            image = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.GRAY);
        }

        // Outside the lock on purpose: the image is a local object, so threads can
        // compress in parallel.
        byte[] jpeg = toJpeg(image, jpegQuality);
        log.debug("page {} at {} dpi -> {} KB", pageIndex, dpi, jpeg.length / 1024);
        return jpeg;
    }

    @Override
    public void close() throws IOException {
        document.close();
    }

    /**
     * Compresses an image to JPEG at a chosen quality.
     *
     * ImageIO.write(image, "jpeg", out) would be shorter but gives no control over
     * quality - it silently uses about 0.75. Going through ImageWriter is the only
     * way to set it.
     */
    private static byte[] toJpeg(BufferedImage image, float jpegQuality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(JPEG_FORMAT);
        if (!writers.hasNext()) {
            throw new IOException("no JPEG writer available in this JRE");
        }
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(jpegQuality);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            // Every ImageWriter holds native buffers. Forgetting this leaks memory -
            // invisible over ten test pages, fatal over six thousand.
            writer.dispose();
        }
        // Read the bytes after the stream is closed, so everything has been flushed.
        return out.toByteArray();
    }
}
