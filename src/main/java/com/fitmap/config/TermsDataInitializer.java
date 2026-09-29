package com.fitmap.config;

import com.fitmap.domain.terms.Terms;
import com.fitmap.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TermsDataInitializer implements ApplicationRunner {

    private final TermsRepository termsRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (termsRepository.count() > 0) return;

        termsRepository.saveAll(List.of(
            Terms.builder()
                .title("서비스 이용약관")
                .content("FitMap은 공공데이터를 기반으로 피트니스 시설 창업 입지 분석 정보를 제공하는 서비스입니다. 제공되는 분석 결과는 참고용이며, 실제 창업 결정에 대한 법적 책임은 이용자에게 있습니다.")
                .version("v1.0")
                .isRequired(true)
                .build(),
            Terms.builder()
                .title("개인정보 수집 및 이용 동의")
                .content("서비스 제공을 위해 소셜 로그인 시 이메일, 닉네임을 수집합니다. 수집된 정보는 서비스 운영 목적으로만 사용되며, 제3자에게 제공하지 않습니다. 회원 탈퇴 시 모든 개인정보는 즉시 파기됩니다.")
                .version("v1.0")
                .isRequired(true)
                .build(),
            Terms.builder()
                .title("크레딧 및 결제 이용약관")
                .content("크레딧은 AI 분석 리포트 열람에 사용됩니다. 결제 완료된 크레딧은 환불이 불가하며, 사용하지 않은 크레딧은 회원 탈퇴 시 소멸됩니다. 현재 서비스는 테스트 환경으로 실제 결제가 발생하지 않습니다.")
                .version("v1.0")
                .isRequired(true)
                .build(),
            Terms.builder()
                .title("서비스 변경 및 중단 안내")
                .content("운영상 또는 기술상의 이유로 서비스 내용을 변경하거나 중단할 수 있습니다. 서비스 중단 시 사전 공지를 원칙으로 하되, 불가피한 경우 사후 공지할 수 있습니다.")
                .version("v1.0")
                .isRequired(false)
                .build(),
            Terms.builder()
                .title("이용자 의무")
                .content("이용자는 타인의 정보를 도용하거나 서비스를 불법적인 목적으로 이용해서는 안 됩니다. 이를 위반할 경우 이용 제한 및 법적 조치를 받을 수 있습니다.")
                .version("v1.0")
                .isRequired(true)
                .build()
        ));
    }
}
