package com.vibelog.global.file;

import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/*
 * [FileStorage]
 * 역할 : 업로드한 파일을 검사해 서버 디스크에 저장하고, 화면에서 열 주소(/uploads/{파일 이름})를 돌려준다. (PL-DAT-004, #79)
 * 책임 : - 이미지: JPG·PNG·WebP, 장당 5MB. 확장자와 실제 파일 형식(파일 맨 앞 바이트)이 다르면 거부한다.
 *        - 설정 파일(평가 에이전트): md·json·yaml·txt, 5MB. 실행 파일은 거부한다. (PL-PRT-010)
 *        - 실패는 모두 BusinessException(INVALID_FILE). 개수 제한(게시글 3장 등)은 쓰는 쪽이 검사한다.
 *        - 저장 파일 이름은 서버가 새로 만든다(UUID). 사용자가 보낸 파일 이름은 확장자를 볼 때만 쓴다.
 *        - 저장 폴더는 환경 변수 UPLOAD_DIR (없으면 프로젝트 폴더의 uploads/). 저장한 파일은 /uploads/** 로 연다.
 */
@Component
public class FileStorage implements WebMvcConfigurer {

    private static final String URL_PREFIX = "/uploads/";
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("md", "json", "yaml", "txt");

    private final Path dir;

    public FileStorage(@Value("${vibelog.upload.dir}") String dir) {
        this.dir = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.dir);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 폴더를 만들 수 없습니다: " + this.dir, e);
        }
    }

    /* 게시글 사진, 포트폴리오 스크린샷, 개념 이미지 */
    public String saveImage(MultipartFile file) {
        byte[] bytes = readWithinLimit(file);
        String extension = extensionOf(file);
        boolean formatMatches = switch (extension) {
            case "jpg", "jpeg" -> startsWith(bytes, 0, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(bytes, 0, 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n');
            case "webp" -> startsWith(bytes, 0, 'R', 'I', 'F', 'F') && startsWith(bytes, 8, 'W', 'E', 'B', 'P');
            default -> false;
        };
        if (!formatMatches) {
            throw new BusinessException(CommonErrorCode.INVALID_FILE);
        }
        return store(bytes, extension);
    }

    /* 평가 에이전트 설정 파일 */
    public String saveDocument(MultipartFile file) {
        byte[] bytes = readWithinLimit(file);
        String extension = extensionOf(file);
        // #! 로 시작하면 실행 스크립트다 (PL-PRT-010 .sh 금지). 이름만 .txt 로 바꾼 것도 막는다
        if (!DOCUMENT_EXTENSIONS.contains(extension) || !isUtf8Text(bytes) || startsWith(bytes, 0, '#', '!')) {
            throw new BusinessException(CommonErrorCode.INVALID_FILE);
        }
        return store(bytes, extension);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(URL_PREFIX + "**").addResourceLocations(dir.toUri().toString());
    }

    private byte[] readWithinLimit(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_FILE);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String extensionOf(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String store(byte[] bytes, String extension) {
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.write(dir.resolve(name), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return URL_PREFIX + name;
    }

    private static boolean startsWith(byte[] bytes, int offset, int... expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /*
     * 설정 파일은 UTF-8 글자 파일만 받는다 (md·json·yaml·txt 는 보통 UTF-8, JSON 은 표준이 UTF-8).
     * 깨진 바이트나 0 바이트가 있으면 글자 파일이 아니다. 실행 파일 이름만 .txt 로 바꾼 것도 여기서 막는다
     */
    private static boolean isUtf8Text(byte[] bytes) {
        try {
            // newDecoder() 는 잘못된 바이트를 만나면 예외를 던진다 (CodingErrorAction.REPORT)
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).chars().noneMatch(c -> c == 0);
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
