package pt.isec.pd.common;

public enum RoleType {
    TEACHER,
    STUDENT;

    public static RoleType fromClass(Object o) {
        if ( o instanceof Teacher ) return TEACHER;
        if (o instanceof Student ) return STUDENT;
        return null;
    }
}
