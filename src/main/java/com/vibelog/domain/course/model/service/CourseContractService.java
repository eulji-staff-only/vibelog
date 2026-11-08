package com.vibelog.domain.course.model.service;

import com.vibelog.domain.course.model.dao.DiagnosisResultRepository;
import com.vibelog.domain.course.model.policy.LoginDiagnosisChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [CourseContractService]
 * 역할 : course 영역이 다른 영역에 주는 계약의 진짜 구현. 지금은 ⑫(LoginDiagnosisChecker)만 있다.
 * 책임 : - 다른 영역은 이 클래스가 아니라 model/policy 의 계약 인터페이스를 주입해 쓴다.
 *        - 로그인 도착지(#12)가 먼저 필요해 만들었다. course 기반(#83)의 다른 계약은 이 클래스에 이어서 구현한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseContractService implements LoginDiagnosisChecker {

    private final DiagnosisResultRepository diagnosisResultRepository;

    /* ⑫ 진단을 마쳤거나 건너뛴 기록이 있음 */
    @Override
    public boolean hasDiagnosisRecord(Long memberId) {
        return diagnosisResultRepository.existsByMemberId(memberId);
    }
}
