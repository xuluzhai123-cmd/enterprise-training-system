-- -------------------------------------------------------------
-- 10. 知识切片表（RAG 用：把章节正文切成 300 字左右的小块）
-- -------------------------------------------------------------
CREATE TABLE knowledge_chunk (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    course_id   BIGINT       NOT NULL COMMENT '所属课程',
    chapter_id  BIGINT       DEFAULT NULL COMMENT '来源章节',
    title       VARCHAR(200) NOT NULL COMMENT '章节标题（用于展示引用）',
    content     TEXT         NOT NULL COMMENT '切片正文',
    seq         INT          NOT NULL DEFAULT 0 COMMENT '本课程内第几块',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    PRIMARY KEY (id),
    KEY idx_course (course_id),
    KEY idx_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='RAG 知识切片';
