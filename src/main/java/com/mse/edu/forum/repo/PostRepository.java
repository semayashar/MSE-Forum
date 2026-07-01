package com.mse.edu.forum.repo;

import com.mse.edu.forum.domain.PostEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

	List<PostEntity> findAllByOrderByCreatedAtAscIdAsc();

	boolean existsByTitle(String title);

	boolean existsByTitleAndIdNot(String title, Long id);
}
