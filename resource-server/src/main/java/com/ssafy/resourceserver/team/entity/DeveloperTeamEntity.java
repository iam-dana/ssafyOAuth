package com.ssafy.resourceserver.team.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ssafy.resourceserver.team.dto.TeamAddDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "developer_team")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperTeamEntity {

	@Id
	@Column(name = "developer_team_seq", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer seq;

	@Column(name = "team_name", nullable = false)
	private String teamName;

	@Column(name = "service_key")
	private String serviceKey;

	@Column(name = "service_name", nullable = false)
	private String serviceName;

	@Column(name = "service_image")
	private String serviceImage;

	@Column(name = "create_date", nullable = false)
	@CreationTimestamp
	private LocalDateTime createDate;

	@Column(name = "delete_date")
	private LocalDateTime deleteDate;

	@Column(name = "modify_date")
	@UpdateTimestamp
	private LocalDateTime modifyDate;

	@Column(name = "is_delete")
	private Boolean isDelete;

	@Column(name = "leader_member_seq", nullable = false)
	private Integer leader;

	@Column(nullable = false, unique = true)
	private String clientId;

	public static DeveloperTeamEntity CreateTeam(TeamAddDto dto,int seq,String secretKey) {
		return DeveloperTeamEntity.builder()
			.clientId(UUID.randomUUID().toString())
			.serviceName(dto.getServiceName())
			.teamName(dto.getTeamName())
			.isDelete(false)
			.leader(seq)
			.serviceKey(secretKey)
			.build();
	}


}
