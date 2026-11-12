package com.etm.dto;

import lombok.Data;

/**
 * RAG 提问入参：针对某门课提问。
 */
@Data
public class RagAskRequest {

    private Long courseId;

    private String question;
}
