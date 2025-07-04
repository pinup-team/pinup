package kr.co.pinup.custom;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import kr.co.pinup.annotation.ValidImageFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ImageFileValidator implements ConstraintValidator<ValidImageFile, List<MultipartFile>> {

    private final List<String> allowedExtensions = List.of("jpg", "jpeg", "png");

    @Override
    public boolean isValid(final List<MultipartFile> files, final ConstraintValidatorContext context) {
        if (files == null || files.isEmpty()) {
            return fail(context, "이미지는 필수 항목입니다.");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                return fail(context, "이미지는 필수 항목입니다.");
            }

            final String filename = file.getOriginalFilename();
            if (filename == null || !isAllowedExtension(filename)) {
                return fail(context, String.format("%s 형식의 이미지 파일만 업로드할 수 있습니다.", allowedExtensions));
            }
        }

        return true;
    }

    private boolean isAllowedExtension(final String filename) {
        final String ext = filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase();

        return allowedExtensions.contains(ext);
    }

    private boolean fail(final ConstraintValidatorContext context, final String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
