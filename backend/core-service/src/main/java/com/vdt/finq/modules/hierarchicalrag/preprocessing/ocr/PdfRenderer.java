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

// mỗi tài liệu một instance, mở bằng try-with-resources
@Slf4j
public final class PdfRenderer implements AutoCloseable {

    private static final String JPEG_FORMAT = "jpeg";

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final Path source;

    private PdfRenderer(PDDocument document, Path source) {
        this.document = document;
        this.source = source;
        // tạo một lần, dùng lại cho mọi trang
        this.renderer = new PDFRenderer(document);
    }

    // mở một file pdf, bên gọi phải tự đóng
    public static PdfRenderer open(Path pdfPath) throws IOException {
        if (!Files.isRegularFile(pdfPath)) {
            throw new IOException("pdf not found: " + pdfPath);
        }
        // pdfbox 3 dùng Loader, pdfbox 2 dùng PDDocument.load
        PDDocument document = Loader.loadPDF(pdfPath.toFile());
        log.debug("opened {} ({} pages)", pdfPath.getFileName(), document.getNumberOfPages());
        return new PdfRenderer(document, pdfPath);
    }

    public int pageCount() {
        return document.getNumberOfPages();
    }

    // kết xuất một trang thành ảnh jpeg xám, pageIndex đếm từ 0
    public byte[] render(int pageIndex, int dpi, float jpegQuality) throws IOException {
        if (pageIndex < 0 || pageIndex >= pageCount()) {
            throw new IllegalArgumentException("page index %d out of range 0..%d in %s"
                    .formatted(pageIndex, pageCount() - 1, source.getFileName()));
        }

        BufferedImage image;
        // PDDocument không an toàn đa luồng nên khoá lại; kết xuất ~200 ms, rẻ so với gọi mạng
        synchronized (document) {
            image = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.GRAY);
        }

        // nén ảnh ngoài khoá, ảnh là biến cục bộ nên các luồng chạy song song được
        byte[] jpeg = toJpeg(image, jpegQuality);
        log.debug("page {} at {} dpi -> {} KB", pageIndex, dpi, jpeg.length / 1024);
        return jpeg;
    }

    @Override
    public void close() throws IOException {
        document.close();
    }

    // nén sang jpeg; ImageIO.write không chỉnh được chất lượng nên phải dùng ImageWriter
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
            // mỗi writer giữ bộ đệm native, rò rỉ thì lượt chạy 6690 trang sẽ chết giữa chừng
            writer.dispose();
        }
        // đọc sau khi đóng stream để chắc mọi byte đã được ghi xong
        return out.toByteArray();
    }
}
