package com.vibelog.global.file;

import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 업로드 파일 규칙(PL-DAT-004, PL-PRT-010)과 저장한 파일이 주소로 열리는지 확인한다. (#79)
 * 규칙 검사는 임시 폴더(@TempDir)에 저장한다. 주소로 여는 확인만 실제 설정(uploads/)을 쓰고 끝나면 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FileStorageTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
    private static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0};

    @TempDir
    Path tempDir;

    @Autowired
    FileStorage fileStorage;

    @Autowired
    MockMvc mockMvc;

    @Value("${vibelog.upload.dir}")
    String uploadDir;

    @Test
    void 이미지는_서버가_새로_만든_이름으로_저장하고_주소를_돌려준다() throws Exception {
        String url = storage().saveImage(file("내 사진.PNG", PNG));

        assertThat(url).matches("/uploads/[0-9a-f-]{36}\\.png");
        assertThat(Files.readAllBytes(tempDir.resolve(url.substring("/uploads/".length())))).isEqualTo(PNG);
        assertThat(storage().saveImage(file("a.jpg", JPEG))).endsWith(".jpg");
        assertThat(storage().saveImage(file("a.jpeg", JPEG))).endsWith(".jpeg");
        assertThat(storage().saveImage(file("a.webp", WEBP))).endsWith(".webp");
    }

    @Test
    void 확장자만_바꾼_이미지는_거부한다() {
        assertInvalid(() -> storage().saveImage(file("a.png", JPEG)));
        assertInvalid(() -> storage().saveImage(file("a.jpg", EXE)));
    }

    @Test
    void 허용하지_않은_형식이나_빈_파일은_거부한다() {
        assertInvalid(() -> storage().saveImage(file("a.gif", PNG)));
        assertInvalid(() -> storage().saveImage(file("확장자없음", PNG)));
        assertInvalid(() -> storage().saveImage(file("a.png", new byte[0])));
    }

    @Test
    void 장당_5MB가_넘는_파일은_거부한다() {
        byte[] justLimit = new byte[5 * 1024 * 1024];
        System.arraycopy(PNG, 0, justLimit, 0, PNG.length);
        byte[] overLimit = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, overLimit, 0, PNG.length);

        assertThat(storage().saveImage(file("a.png", justLimit))).endsWith(".png");
        assertInvalid(() -> storage().saveImage(file("a.png", overLimit)));
        assertInvalid(() -> storage().saveDocument(file("a.md", new byte[5 * 1024 * 1024 + 1])));
    }

    @Test
    void 설정_파일은_md_json_yaml_txt만_받고_실행_파일은_거부한다() {
        byte[] text = "# 평가 에이전트".getBytes();

        assertThat(storage().saveDocument(file("agent.md", text))).endsWith(".md");
        assertThat(storage().saveDocument(file("agent.yaml", text))).endsWith(".yaml");
        assertInvalid(() -> storage().saveDocument(file("run.sh", text)));
        assertInvalid(() -> storage().saveDocument(file("app.exe", EXE)));
        assertInvalid(() -> storage().saveDocument(file("app.txt", EXE)));
    }

    @Test
    void 저장한_이미지는_비회원도_돌려받은_주소로_연다() throws Exception {
        String url = fileStorage.saveImage(file("a.png", PNG));
        try {
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/png"))
                    .andExpect(content().bytes(PNG));
        } finally {
            Files.delete(Path.of(uploadDir, url.substring("/uploads/".length())));
        }
    }

    private FileStorage storage() {
        return new FileStorage(tempDir.toString());
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("image", name, "application/octet-stream", bytes);
    }

    private static void assertInvalid(Runnable save) {
        assertThatThrownBy(save::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.INVALID_FILE);
    }
}
