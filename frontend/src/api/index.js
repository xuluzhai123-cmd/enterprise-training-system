import request from './request'

// ---------------- 认证 ----------------
export const login = data => request.post('/auth/login', data)
export const getMe = () => request.get('/auth/me')
export const logout = () => request.post('/auth/logout')

// ---------------- 部门 ----------------
export const deptTree = () => request.get('/dept/tree')
export const deptList = () => request.get('/dept/list')
export const createDept = data => request.post('/dept', data)
export const updateDept = data => request.put('/dept', data)
export const deleteDept = id => request.delete(`/dept/${id}`)

// ---------------- 员工 ----------------
export const userPage = params => request.get('/user/page', { params })
export const userOptions = () => request.get('/user/options')
export const createUser = data => request.post('/user', data)
export const updateUser = data => request.put('/user', data)
export const deleteUser = id => request.delete(`/user/${id}`)
export const updateUserStatus = (id, status) => request.put(`/user/${id}/status`, null, { params: { status } })
export const resetUserPassword = id => request.put(`/user/${id}/reset`)

// ---------------- 课程 ----------------
export const coursePage = params => request.get('/course/page', { params })
export const courseOptions = () => request.get('/course/options')
export const courseDetail = id => request.get(`/course/${id}`)
export const createCourse = data => request.post('/course', data)
export const updateCourse = data => request.put('/course', data)
export const deleteCourse = id => request.delete(`/course/${id}`)

// ---------------- 章节 ----------------
export const chapterList = courseId => request.get('/chapter/list', { params: { courseId } })
export const createChapter = data => request.post('/chapter', data)
export const updateChapter = data => request.put('/chapter', data)
export const deleteChapter = id => request.delete(`/chapter/${id}`)

// ---------------- 培训班 ----------------
export const classPage = params => request.get('/class/page', { params })
export const classOptions = () => request.get('/class/options')
export const createClass = data => request.post('/class', data)
export const updateClass = data => request.put('/class', data)
export const updateClassStatus = (id, status) => request.put(`/class/${id}/status`, null, { params: { status } })
export const deleteClass = id => request.delete(`/class/${id}`)

// ---------------- 报名 ----------------
export const enrollPage = params => request.get('/enroll/page', { params })
export const myEnroll = () => request.get('/enroll/my')
export const applyEnroll = classId => request.post(`/enroll/${classId}`)
export const approveEnroll = id => request.put(`/enroll/${id}/approve`)
export const rejectEnroll = id => request.put(`/enroll/${id}/reject`)
export const cancelEnroll = id => request.put(`/enroll/${id}/cancel`)

// ---------------- 学习 ----------------
export const learnDetail = enrollId => request.get(`/learn/${enrollId}`)
export const updateProgress = (enrollId, progress) =>
  request.put(`/learn/${enrollId}/progress`, null, { params: { progress } })
// 勾选/取消章节完成，返回后端重算后的进度
export const toggleLearnChapter = (enrollId, chapterId, done = true) =>
  request.post(`/learn/${enrollId}/chapter/${chapterId}`, null, { params: { done } })

// ---------------- 题库 ----------------
export const questionPage = params => request.get('/question/page', { params })
export const createQuestion = data => request.post('/question', data)
export const updateQuestion = data => request.put('/question', data)
export const deleteQuestion = id => request.delete(`/question/${id}`)

// ---------------- 考试 ----------------
export const startExam = classId => request.post('/exam/start', null, { params: { classId } })
export const submitExam = data => request.post('/exam/submit', data)
export const examRecords = params => request.get('/exam/records', { params })

// ---------------- 统计 ----------------
export const dashboard = () => request.get('/stat/dashboard')

// ---------------- AI ----------------
// 生成题目：只返回给前端预览，不落库
export const aiGenerateQuestions = data => request.post('/ai/question/generate', data)
// 把勾选的题目存进题库
export const aiSaveQuestions = data => request.post('/ai/question/save', data)

// 生成一个学员的学习评语（已有则直接返回，force=true 重新生成）
export const aiGenerateComment = data => request.post('/ai/comment/generate', data)
// 给一个班还没有评语的学员批量生成（limit 最多 20）
export const aiBatchComment = (classId, limit = 5) => request.post('/ai/comment/batch', null, { params: { classId, limit } })

// 课程问答（RAG）：向某门课的助教提问
export const ragAsk = data => request.post('/ai/rag/ask', data)
// 为一门课构建知识库（把章节正文切片入库）
export const ragIngest = courseId => request.post('/ai/rag/ingest', null, { params: { courseId } })
// 查这门课已经有多少块知识切片
export const ragStatus = courseId => request.get('/ai/rag/status', { params: { courseId } })
