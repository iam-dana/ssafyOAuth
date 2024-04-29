package com.ssafy.authorization.stats.login.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ssafy.authorization.stats.login.model.LoginStats;
import com.ssafy.authorization.stats.login.model.vo.LoginStatsFetchRequestVO;
import com.ssafy.authorization.stats.login.repository.LoginStatsRepository;

@Service
public class LoginStatsServiceImpl implements LoginStatsService {

	private final LoginStatsRepository loginStatsRepository;

	@Autowired
	LoginStatsServiceImpl(LoginStatsRepository loginStatsRepository) {
		this.loginStatsRepository = loginStatsRepository;
	}

	@Override
	public void save(LoginStats loginStats) {
		loginStatsRepository.create(loginStats);
	}

	@Override
	public void delete(String userId, String teamId) {
		return;
	}

	@Override
	public List<LoginStats> fetch(LoginStatsFetchRequestVO requestVO, Pageable pageable) {
		return loginStatsRepository.fetch(requestVO, pageable);
	}
}
