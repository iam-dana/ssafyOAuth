package com.ssafy.authorization.team.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.text.html.parser.Entity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.authorization.team.dto.TeamAddDto;
import com.ssafy.authorization.team.entity.DeveloperMemberEntity;
import com.ssafy.authorization.team.entity.DeveloperTeamEntity;
import com.ssafy.authorization.team.entity.TeamListEntity;
import com.ssafy.authorization.team.entity.TeamMemberEntity;
import com.ssafy.authorization.team.entity.TeamMemberPK;
import com.ssafy.authorization.team.entity.TeamMemberWithInfoEntity;
import com.ssafy.authorization.team.repository.DeveloperMemberRepository;
import com.ssafy.authorization.team.repository.DeveloperTeamRepository;
import com.ssafy.authorization.team.repository.TeamListRepository;
import com.ssafy.authorization.team.repository.TeamMemberRepository;
import com.ssafy.authorization.team.repository.TeamMemberWithInfoRepository;
import com.ssafy.authorization.team.vo.DeveloperSearchVo;
import com.ssafy.authorization.team.vo.ServiceNameUpdateVo;
import com.ssafy.authorization.team.vo.TeamAddVo;
import com.ssafy.authorization.team.vo.TeamDetailVo;
import com.ssafy.authorization.team.vo.TeamListVo;
import com.ssafy.authorization.team.vo.TeamMemberVo;
import com.ssafy.authorization.team.vo.TeamNameUpdateVo;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService{
	private final DeveloperTeamRepository developerTeamRepository;

	private final TeamMemberRepository teamMemberRepository;

	private final TeamListRepository teamListRepository;

	private final TeamMemberWithInfoRepository teamMemberWithInfoRepository;

	private final DeveloperMemberRepository developerMemberRepository;

	@Override
	@Transactional
	public Map<String, Object> addTeam(TeamAddVo vo) {
		Map<String, Object> data = new HashMap<>();
		if(vo.getTeamMember().length > 5){
			data.put("msg", "팀원은 자신 포함 6명을 넘길 수 없습니다.");
			data.put("team_seq", null);
			return data;
		}
		if(vo.getDomainUrl().length > 5){
			data.put("msg", "도메인은 5개까지 등록할 수 있습니다.");
			data.put("team_seq", null);
			return data;
		}
		if(vo.getRedirectionUrl().length > 10){
			data.put("msg", "리다이렉트 url은 10개까지 등록할 수 있습니다.");
			data.put("team_seq", null);
			return data;
		}
		// 팀원들이 모두 등록된 개발자 인지 확인
		for(String member : vo.getTeamMember()){
			boolean is_exist = true;
			// 존재 하지 않는 팀 원이면
			if(!is_exist){
				data.put("msg", "팀원 목록에 존재 하지 않는 개발자 이메일이 있습니다.");
				data.put("team_seq", null);
				return data;
			}
		}
		TeamAddDto dto = new TeamAddDto();
		dto.setTeamName(vo.getTeamName());
		dto.setServiceName(vo.getServiceName());
		String[] teamMembers = new String[vo.getTeamMember().length + 1];
		for(int i = 0; i < vo.getTeamMember().length; i++){
			teamMembers[i] = vo.getTeamMember()[i];
		}
		// 자기 자신의 email을 추가
		String myEmail = "자기 자신의 이메일 검색 해서 대입";
		teamMembers[vo.getTeamMember().length] = myEmail;
		dto.setDomainUrl(vo.getDomainUrl());
		dto.setRedirectUrl(vo.getRedirectionUrl());
		// 자기 자신의 seq를 team leader seq로 지정
		int mySeq = 0;
		dto.setLeaderMemberSeq(mySeq);

		// 팀 생성
		DeveloperTeamEntity entity = new DeveloperTeamEntity();
		entity.setServiceName(dto.getServiceName());
		entity.setTeamName(dto.getTeamName());
		entity.setServiceName(dto.getServiceName());
		Integer teamSeq = developerTeamRepository.save(entity).getSeq();

		// 팀원 생성
		for(String email : dto.getMembers()){
			// email로 member_seq읽기
			Integer seq = 0;
			//멤버로 추가
			teamMemberRepository.save(new TeamMemberEntity(teamSeq, seq, seq == mySeq ? true : false));
		}
		// 도메인 등록

		// 리다이렉트 url 등록

		data.put("msg", null);
		data.put("team_seq", teamSeq);
		return data;
	}

	@Override
	@Transactional
	public Map deleteTeam(Integer teamSeq) {
		Map<String, String> data = new HashMap<>();
		// 요청된 팀이 존재하는지 확인
		List<DeveloperTeamEntity> list = developerTeamRepository.findBySeqAndIsDeleteFalse(teamSeq);
		if(list.size() != 1){
			data.put("msg", "존재 하지 않는 팀");
			return data;
		}
		// 요청한 사람의 시퀀스 넘버 확인
		Integer mySeq = 0;
		// 요청한 사람이 팀의 리더 인지 파악
		DeveloperTeamEntity entity = list.get(0);
		if(entity.getLeader() != mySeq){
			data.put("msg", "삭제 권한이 없습니다.");
			return data;
		}
		entity.setIsDeleted(true);
		entity.setDeleteDate(LocalDateTime.now());
		developerTeamRepository.save(entity);
		data.put("msg", "삭제되었습니다.");
;		return data;
	}

	@Override
	@Transactional
	public Map updateTeamName(Integer teamSeq, TeamNameUpdateVo vo) {
		Map<String, String> data = new HashMap<>();

		// 요청된 팀이 존재 하는지 확인
		List<DeveloperTeamEntity> list = developerTeamRepository.findBySeqAndIsDeleteFalse(teamSeq);
		if(list.size() != 1){
			data.put("msg", "존재하지 않는 팀");
			data.put("team_name", null);
			return data;
		}

		// 자신의 시퀀스 넘버 확인
		Integer mySeq = 0;

		// 자신이 요청한 팀의 팀원인지 확인
		Optional<TeamMemberEntity> member = teamMemberRepository.findById(new TeamMemberPK(teamSeq, mySeq));
		if(member.isEmpty()){
			data.put("msg", "팀명을 수정할 수 있는 권한이 없습니다.");
			data.put("team_name", null);
			return data;
		}

		// 팀 명 수정
		DeveloperTeamEntity entity = list.get(0);
		entity.setTeamName(vo.getTeamName());
		entity = developerTeamRepository.save(entity);
		data.put("msg", null);
		data.put("team_name", entity.getTeamName());
		return data;
	}

	@Override
	@Transactional(readOnly = true)
	public Map listTeam() {
		Map<String, Object> data = new HashMap<>();
		// 자신의 시퀀스 넘버를 확인
		Integer mySeq = 0;
		List<TeamListEntity> entities = teamListRepository.findByMemberSeq(mySeq);
		if(entities.isEmpty()){
			data.put("msg", "소속된 팀이 존재하지 않습니다.");
			data.put("list", null);
			return data;
		}
		List<TeamListVo> vos = entities.stream().map(entity ->{
			TeamListVo vo = new TeamListVo();
			vo.setTeamName(entity.getTeamName());
			vo.setServiceName(entity.getServiceName());
			vo.setTeamSeq(entity.getTeamSeq());
			vo.setIsLeader(mySeq == entity.getLeader() ? true : false);
			vo.setIsAccept(entity.getIsAccept());
			vo.setCreateDate(entity.getCreateDate());
			vo.setModifyDate(entity.getModifyDate());
			return vo;
		}).collect(Collectors.toList());
		data.put("msg", null);
		data.put("list", vos);
		return data;
	}

	@Override
	@Transactional(readOnly = true)
	public Map detailTeam(Integer teamSeq) {
		Map<String, Object> data = new HashMap<>();
		// 존재 하는 팀인지 확인
		List<DeveloperTeamEntity> list = developerTeamRepository.findBySeqAndIsDeleteFalse(teamSeq);
		if(list.size() != 1){
			data.put("msg", "존재하지 않는 팀");
			data.put("team_name", null);
			return data;
		}
		// 요청을 한 사람이 팀 원인지 확인
		Integer mySeq = 0;
		Optional<TeamMemberEntity> member = teamMemberRepository.findById(new TeamMemberPK(teamSeq, mySeq));
		if(member.isEmpty()){
			data.put("msg", "팀을 볼 수 있는 권한이 없습니다.");
			data.put("team_name", null);
			return data;
		}
		// 정상 응답의 경우
		DeveloperTeamEntity team = list.get(0);
		TeamDetailVo vo = new TeamDetailVo();
		vo.setTeamName(team.getTeamName());
		vo.setServiceName(team.getServiceName());
		vo.setServiceKey(team.getServiceKey());
		List<TeamMemberVo> memberList = teamMemberWithInfoRepository.findAllByTeamSeq(teamSeq).stream().map(entity -> {
			TeamMemberVo memberVo = new TeamMemberVo();
			memberVo.setEmail(entity.getEmail());
			memberVo.setName(entity.getName());
			memberVo.setIsAccept(entity.getIsAccept());
			memberVo.setIsLeader(entity.getMemberSeq() == team.getLeader() ? true : false);
			return memberVo;
		}).collect(Collectors.toList());
		vo.setMembers(memberList);
		// 도메인과 리디이렉트 url 셋팅

		//응답
		data.put("msg", null);
		data.put("team", vo);
		return data;
	}

	@Override
	@Transactional
	public Map updateServiceName(Integer teamSeq, ServiceNameUpdateVo vo) {
		Map<String, String> data = new HashMap<>();
		// 팀이 존재 하는지 확인
		List<DeveloperTeamEntity> list = developerTeamRepository.findBySeqAndIsDeleteFalse(teamSeq);
		if(list.size() != 1){
			data.put("msg", "존재하지 않는 팀");
			data.put("team_name", null);
			return data;
		}
		// 사용자가 팀의 멤버인지 확인
		Integer mySeq = 0;
		Optional<TeamMemberEntity> member = teamMemberRepository.findById(new TeamMemberPK(teamSeq, mySeq));
		if(member.isEmpty()){
			data.put("msg", "팀을 볼 수 있는 권한이 없습니다.");
			data.put("team_name", null);
			return data;
		}
		// 팀의 이름 변경
		DeveloperTeamEntity team = list.get(0);
		team.setServiceName(vo.getServiceName());
		team = developerTeamRepository.save(team);
		// 정상 응답
		data.put("serviceName", team.getServiceName());
		data.put("msg",null);
		return data;
	}

	@Override
	@Transactional(readOnly = true)
	public Map searchDeveloper(String email) {
		Map<String, Object> data = new HashMap<>();
		List<DeveloperMemberEntity> list = developerMemberRepository.findAllByEmailContains(email);
		if(list.size() == 0){
			data.put("list", null);
			data.put("msg", "검색 결과 없음");
			return data;
		}
		List<DeveloperSearchVo> vos = list.stream().map(entity -> {
			return new DeveloperSearchVo(entity.getEmail(), entity.getName(), entity.getIamge());
		}).collect(Collectors.toList());
		data.put("msg", null);
		data.put("list", vos);
		return data;
	}

	@Override
	@Transactional
	public Map addMember(Integer teamSeq, String email) {
		Map<String, Object> data = new HashMap<>();
		// 팀에 멤버를 추가할 권한이 있는지 확인
		Integer mySeq = 0;
		Optional<TeamMemberEntity> isMember = teamMemberRepository.findById(new TeamMemberPK(teamSeq, mySeq));
		if(isMember.isEmpty()){
			data.put("msg", "팀에 멤버를 추가할 권한이 없습니다");
			data.put("member", null);
			return data;
		}
		// 팀에 이미 추가된 멤버인지 확인
		List<TeamMemberWithInfoEntity> isTeamMember = teamMemberWithInfoRepository.findAllByTeamSeqAndEmail(teamSeq, email);
		if(!isTeamMember.isEmpty()){
			data.put("msg", "이미 팀에 멤버로 추가된 개발자 입니다.");
			data.put("member", null);
			return data;
		}
		// 팀에 멤버를 추가 할 수 있는 자리가 있는지 확인
		Integer cnt = teamMemberRepository.countByTeamSeq(teamSeq);
		if(cnt >= 6){
			data.put("msg", "한 팀에 멤버는 최대 6명 입니다. 멤버를 추가 하려면 기존 멤버를 지워 주세요");
			data.put("member", null);
			return data;
		}
		// 해당 개발자의 시퀀스 넘버 확인
		List<DeveloperMemberEntity> dm = developerMemberRepository.findAllByEmail(email);
		if(dm.isEmpty()){
			data.put("msg", "개발자로 등록된 이메일이 아닙니다.");
			data.put("member", null);
			return data;
		}
		Integer memberSeq = dm.get(0).getMemberSeq();
		// 팀에 멤버 추가
		TeamMemberEntity teamMemberEntity = teamMemberRepository.save(new TeamMemberEntity(teamSeq, memberSeq, false));
		DeveloperMemberEntity e = dm.get(0);
		data.put("msg", null);
		data.put("member", new DeveloperSearchVo(e.getEmail(), e.getName(), e.getIamge()));
		return data;
	}

	@Override
	@Transactional
	public Map deleteMember(Integer teamSeq, String email) {
		Map<String, Object> data = new HashMap<>();
		// 팀에 멤버를 삭제할 권한이 있는지 확인
		Integer mySeq = 0;
		Optional<TeamMemberEntity> isMember = teamMemberRepository.findById(new TeamMemberPK(teamSeq, mySeq));
		if(isMember.isEmpty()){
			data.put("msg", "팀 멤버를 수정할 권한이 없습니다");
			return data;
		}
		// 해당 이메일의 멤버가
		// 팀에 포함된 멤버인지 확인
		List<TeamMemberWithInfoEntity> list = teamMemberWithInfoRepository.findAllByTeamSeqAndEmail(teamSeq, email);
		if(list.isEmpty()){
			data.put("msg", "해당 이메일의 개발자는  팀 멤버가 아닙니다.");
			return data;
		}
		// 팀의 리더인지 확인 -> 리더는 삭제 될 수 없음
		Optional<DeveloperTeamEntity> teamOptional = developerTeamRepository.findById(teamSeq);
		if(teamOptional.isEmpty()){
			data.put("msg", "팀이 존재하지 않습니다.");
			return data;
		}
		DeveloperTeamEntity team = teamOptional.get();
		Integer leaderSeq = team.getLeader();
		Optional<DeveloperMemberEntity> dmOptional = developerMemberRepository.findById(leaderSeq);
		if(!dmOptional.isEmpty()) {
			DeveloperMemberEntity dm = dmOptional.get();
			if (dm.getEmail() == email) {
				data.put("msg", "팀 리더는 삭제할 수 없습니다.");
				return data;
			}
		}
		// 삭제할 멤버의 시퀀스 넘버 확인
		Integer memberSeq = list.get(0).getMemberSeq();
		// 팀에서 삭제
		teamMemberRepository.deleteById(new TeamMemberPK(teamSeq, memberSeq));
		data.put("msg", "정상 삭제 되었습니다.");
		return data;
	}
}
