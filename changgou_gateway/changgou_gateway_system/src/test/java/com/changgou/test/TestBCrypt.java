package com.changgou.test;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class TestBCrypt {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            //获取盐
            String gensalt = BCrypt.gensalt();

            System.out.println("gensalt = " + gensalt);
            //基于当前的盐对密码进行加密
            String saltPassword = BCrypt.hashpw("123456", gensalt);

            System.out.println("saltPassword = " + saltPassword);

            //解密
            boolean checkpw = BCrypt.checkpw("123456", saltPassword);
            System.out.println("checkpw = " + checkpw);
        }
    }
}
