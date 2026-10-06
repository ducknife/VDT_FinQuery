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

// one instance per document, open it with try-with-resources
@Slf4j
public final class PdfRenderer implements AutoCloseable {

    private static final String JPEG_FORMAT = "jpeg";

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final Path source;

    private PdfRenderer(PDDocument document, Path source) {
        this.document = document;
        this.source = source;
        // built once and reused for every page
        this.renderer = new PDFRenderer(document);
    }

    // open a pdf, caller must close it
    public static PdfRenderer open(Path pdfPath) throws IOException {
        if (!Files.isRegularFile(pdfPath)) {
            throw new IOException("pdf not found: " + pdfPath);
        }
        // pdfbox 3 uses Loader, pdfbox 2 used PDDocument.load
        PDDocument document = Loader.loadPDF(pdfPath.toFile());
        log.debug("opened {} ({} pages)", pdfPath.getFileName(), document.getNumberOfPages());
        return new PdfRenderer(document, pdfPath);
    }

    public int pageCount() {
        return document.getNumberOfPages();
    }

    // render one page to a grayscale jpeg, pageIndex starts at 0
    public byte[] render(int pageIndex, int dpi, float jpegQuality) throws IOException {
        if (pageIndex < 0 || pageIndex >= pageCount()) {
            throw new IllegalArgumentException("page index %d out of range 0..%d in %s"
                    .formatted(pageIndex, pageCount() - 1, source.getFileName()));
        }

        BufferedImage image;
        // PDDocument is not thread safe, so only one thread may render at a time.
        // cheap to serialise: rendering is ~200ms, the network call is 20-120s
        synchronized (document) {
            image = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.GRAY);
        }

        // compress outside the lock, the image is local so threads can run in parallel
        byte[] jpeg = toJpeg(image, jpegQuality);
        log.debug("page {} at {} dpi -> {} KB", pageIndex, dpi, jpeg.length / 1024);
        return jpeg;
    }

    @Override
    public void close() throws IOException {
        document.close();
    }

    // compress to jpeg, ImageIO.write cannot set the quality so we go through ImageWriter
    private static byte[] toJpeg(BufferedImage image, float jpegQuality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(JPEG_FORMAT);
        if (!writers.hasNext()) {
            throw new IOException("no jpeg writer in this jre");
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
            // every writer holds native buffers, leaking them kills a 6690 page run
            writer.dispose();
        }
        // read after the stream is closed so everything has been flushed
        return out.toByteArray();
    }
}
