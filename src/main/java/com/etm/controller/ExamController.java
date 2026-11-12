package com.etm.controller;

import com.etm.common.PageResult;
import com.etm.common.Result;
import com.etm.dto.ExamSubmitDTO;
import com.etm.entity.ExamRecord;
import com.etm.service.ExamService;
import com.etm.vo.ExamResultVO;
import com.etm.vo.ExamStartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    /** 开始考试，返回随机题目（不含答案） */
    @PostMapping("/start")
    public Result<ExamStartVO> start(@RequestParam Long classId) {
        return Result.ok(examService.start(classId));
    }

    /** 交卷 */
    @PostMapping("/submit")
    public Result<ExamResultVO> submit(@RequestBody ExamSubmitDTO dto) {
        return Result.ok(examService.submit(dto));
    }

    /** 成绩列表（管理员/讲师看全部，学员只能看自己） */
    @GetMapping("/records")
    public Result<PageResult<ExamRecord>> records(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) Long classId,
                                                  @RequestParam(defaultValue = "false") boolean mine) {
        return Result.ok(examService.records(current, size, classId, mine));
    }
}
