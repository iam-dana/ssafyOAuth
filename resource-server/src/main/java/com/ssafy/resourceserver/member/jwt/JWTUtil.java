package com.ssafy.resourceserver.member.jwt;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.StringTokenizer;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JWTUtil {

    // private Key key;
    //
    // public JWTUtil(@Value("${spring.jwt.secret}")String secret) {
    //     byte[] byteSecretKey = Decoders.BASE64.decode(secret);
    //     key = Keys.hmacShaKeyFor(byteSecretKey);
    // }
    //
    // public String getEmail(String token) {
    //
    //     return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("sub", String.class);
    // }
    //
    // public String getScope(String token) {
    //
    //     return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("scope", String.class);
    // }
    //
    // public Boolean isExpired(String token) {
    //
    //     return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getExpiration().before(new Date());
    // }

    public long getExp(String token) throws JSONException {
        StringTokenizer st = new StringTokenizer(token, ".");
        String header = st.nextToken();
        String payload = st.nextToken();
        String signature = st.nextToken();

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decode = decoder.decode(payload);
        // String으로 변환
        String payloadString = new String(decode);

        // JSON 파싱
        JSONObject payloadJson = new JSONObject(payloadString);

        // exp 값 추출
        return payloadJson.getLong("exp");
    }
}