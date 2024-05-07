package com.ssafy.resourceserver.member.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.ssafy.resourceserver.member.model.dto.ProfileInformationForUpdatesDto;
import com.ssafy.resourceserver.member.model.dto.UserInfo;

public interface MemberService {

    Map<String, Object> getUserProfile(String email, List<String> scopes);

    void updateUserProfile(String email, ProfileInformationForUpdatesDto dto) throws IOException;

    void updateSecurityGrade(String email, Integer grade);

    void updatePassword(String email, Map<String, String> passwords);


    ProfileInformationForUpdatesDto ProfileInforForUpdatesData(String email);
}
