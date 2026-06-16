package com.example.yesmaam.ui.nav

object Routes {
    const val CLASSES = "classes"
    const val SETTINGS = "settings"
    fun classEditor(classId: Long?) = "classEditor?classId=${classId ?: -1L}"
    const val CLASS_EDITOR = "classEditor?classId={classId}"
    fun classHome(classId: Long) = "class/$classId"
    const val CLASS_HOME = "class/{classId}"
    fun studentEditor(classId: Long, studentId: Long?) =
        "studentEditor?classId=$classId&studentId=${studentId ?: -1L}"
    const val STUDENT_EDITOR = "studentEditor?classId={classId}&studentId={studentId}"
}
