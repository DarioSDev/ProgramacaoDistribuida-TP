package pt.isec.pd.common.core;

import pt.isec.pd.common.entities.Student;
import pt.isec.pd.common.entities.Teacher;

public enum RoleType {
    TEACHER,
    STUDENT,
    NONE;

    public static RoleType fromClass(Object o) {
        System.out.println("TEACHER");
        if ( o instanceof Teacher) return TEACHER;
        System.out.println("STUDENT");
        if (o instanceof Student) return STUDENT;
        System.out.println("NONE");
        return NONE;
    }
}
