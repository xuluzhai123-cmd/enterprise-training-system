package com.etm;

import java.util.ArrayList;
import java.util.List;

public class Test {

    static class Student {
        String name;
        int score;

        Student(String name, int score) {
            this.name = name;
            this.score = score;
        }

        boolean pass() {
            return score >= 60;
        }
    }

    public static void main(String[] args) {
        List<Student> list = new ArrayList<>();
        list.add(new Student("张三", 25));
        list.add(new Student("李四", 52));
        list.add(new Student("王五", 60));

        list.sort((a, b) -> b.score - a.score);
        for (Student s : list) {
            System.out.println(s.name + ":"  + s.score);
        }


    }
}