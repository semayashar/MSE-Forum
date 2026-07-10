package com.mse.edu.forum.mapper;

import com.mse.edu.forum.api.model.CreateReplyRequest;
import com.mse.edu.forum.api.model.ReplyResponse;
import com.mse.edu.forum.api.model.UpdateReplyRequest;
import com.mse.edu.forum.domain.ReplyEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface ReplyMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "author", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "postId", source = "postId")
	@Mapping(target = "content", source = "request.content", qualifiedByName = "replyTrimmed")
	ReplyEntity toEntity(CreateReplyRequest request, Long postId);

	@Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "replyInstantToOffset")
	@Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "replyInstantToOffset")
	ReplyResponse toResponse(ReplyEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "postId", ignore = true)
	@Mapping(target = "author", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "content", source = "content", qualifiedByName = "replyTrimmed")
	void applyUpdate(UpdateReplyRequest request, @MappingTarget ReplyEntity entity);

	@Named("replyTrimmed")
	default String trimmed(String value) {
		return value == null ? null : value.trim();
	}

	@Named("replyInstantToOffset")
	default OffsetDateTime instantToOffset(Instant instant) {
		return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
	}
}
