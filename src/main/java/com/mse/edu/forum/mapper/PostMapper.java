package com.mse.edu.forum.mapper;

import com.mse.edu.forum.api.generated.model.CreatePostRequest;
import com.mse.edu.forum.api.generated.model.PostDetailsResponse;
import com.mse.edu.forum.api.generated.model.PostResponse;
import com.mse.edu.forum.api.generated.model.UpdatePostRequest;
import com.mse.edu.forum.domain.PostEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface PostMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "author", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "viewCount", ignore = true)
	@Mapping(target = "title", source = "title", qualifiedByName = "postTrimmed")
	@Mapping(target = "content", source = "content", qualifiedByName = "postTrimmed")
	PostEntity toEntity(CreatePostRequest request);

	@Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "postInstantToOffset")
	@Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "postInstantToOffset")
	PostResponse toResponse(PostEntity entity);

	@Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "postInstantToOffset")
	@Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "postInstantToOffset")
	@Mapping(target = "replies", ignore = true)
	PostDetailsResponse toDetailsResponse(PostEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "author", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "viewCount", ignore = true)
	@Mapping(target = "title", source = "title", qualifiedByName = "postTrimmed")
	@Mapping(target = "content", source = "content", qualifiedByName = "postTrimmed")
	void applyUpdate(UpdatePostRequest request, @MappingTarget PostEntity entity);

	@Named("postTrimmed")
	default String trimmed(String value) {
		return value == null ? null : value.trim();
	}

	@Named("postInstantToOffset")
	default OffsetDateTime instantToOffset(Instant instant) {
		return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
	}
}
