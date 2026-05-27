package com.lingring.domain.expression.dao;

import com.lingring.domain.expression.domain.Icebreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IcebreakerRepository extends JpaRepository<Icebreaker, Long> {

    @Query(value = "SELECT * FROM icebreaker ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Icebreaker> findRandom(@Param("count") int count);
}
