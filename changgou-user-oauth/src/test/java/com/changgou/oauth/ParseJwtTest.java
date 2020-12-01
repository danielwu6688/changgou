package com.changgou.oauth;

import org.junit.Test;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

public class ParseJwtTest {

    @Test
    public void parseJWT(){
        //基于公钥解析JWT
        //jwt令牌
        String jwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6WyJhcHAiXSwibmFtZSI6bnVsbCwiaWQiOm51bGwsImV4cCI6MTYwNjU4Mzc3NywiYXV0aG9yaXRpZXMiOlsic2Vja2lsbF9saXN0IiwidXNlciIsImdvb2RzX2xpc3QiXSwianRpIjoiNDhhYTNiYTctYmRkNi00MzFkLWJkNWItYzJhNThlNDE2YjdjIiwiY2xpZW50X2lkIjoiY2hhbmdnb3UiLCJ1c2VybmFtZSI6ImhlaW1hIn0.r3coyVU05ef1NcZJvZ8CTDc_UveFSaCkYxP5tt6Yqg3c3Jvzotx2_dBaOuIrouxF0UYUF7xW1vRsaIgicallHuLOK0xtc5Jb5LiAWMS9DH-rN04epxRuhNiru1N8TrzA4m9U5LPGnQa-LL8yX1ABKEUfx-kuRlkz1quw4uN5cznQWknCyJzMdV_cjS98PHUv9NH8pkn7lvc9mR9xS_slFZESgJHGBQzzD2faeUlfrwcUuhGEAXs1JwBDm1XAzGzbesKl0lHxKkGUXK8nlwkicuYdugYQkzmv3Vqb-jyf1UjHK3g2HVZh3UAMVxjluVfZCUKkRg3lZyxNjjMjOfjzvg";
        //公钥
        String publicKey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvFsEiaLvij9C1Mz+oyAmt47whAaRkRu/8kePM+X8760UGU0RMwGti6Z9y3LQ0RvK6I0brXmbGB/RsN38PVnhcP8ZfxGUH26kX0RK+tlrxcrG+HkPYOH4XPAL8Q1lu1n9x3tLcIPxq8ZZtuIyKYEmoLKyMsvTviG5flTpDprT25unWgE4md1kthRWXOnfWHATVY7Y/r4obiOL1mS5bEa/iNKotQNnvIAKtjBM4RlIDWMa6dmz+lHtLtqDD2LF1qwoiSIHI75LQZ/CNYaHCfZSxtOydpNKq8eb1/PGiLNolD4La2zf0/1dlcr5mkesV570NxRmU1tFm8Zd3MZlZmyv9QIDAQAB-----END PUBLIC KEY-----";

        //解析令牌
        Jwt token = JwtHelper.decodeAndVerify(jwt, new RsaVerifier(publicKey));

        //获取令牌的值
        String claims = token.getClaims();

        System.out.println("claims = " + claims);

    }
}
