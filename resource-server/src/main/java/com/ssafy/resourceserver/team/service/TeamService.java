package com.ssafy.resourceserver.team.service;

import java.util.Map;

import com.ssafy.resourceserver.team.vo.ServiceNameUpdateVo;
import com.ssafy.resourceserver.team.vo.TeamAddVo;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.resourceserver.team.vo.TeamImageVo;
import com.ssafy.resourceserver.team.vo.TeamNameUpdateVo;

public interface TeamService {
	Map<String, Object> addTeam(TeamAddVo vo, String email);

	Map deleteTeam(Integer teamSeq, Authentication authentication);

	Map updateTeamName(Integer teamSeq, TeamNameUpdateVo vo, Authentication authentication);

	Map listTeam(String email);

	Map detailTeam(Integer teamSeq, Authentication authentication);

	Map updateServiceName(Integer teamSeq, ServiceNameUpdateVo vo, Authentication authentication);

	Map searchDeveloper(String email);

	Map addMember(Integer teamSeq, String email, Authentication authentication);

	Map deleteMember(Integer teamSeq, String email, Authentication authentication);

	Map uploadTeamImage(MultipartFile file);

	Map deleteTeamImage(Integer teamSeq, Authentication authentication);

	Map modifyTeamImage(Integer teamSeq, TeamImageVo vo, Authentication authentication);

	Map listInvitedTeam(Authentication authentication);

	Map acceptInvite(Integer teamSeq, Authentication authentication);

	Map rejectInvite(Integer teamSeq, Authentication authentication);
}
