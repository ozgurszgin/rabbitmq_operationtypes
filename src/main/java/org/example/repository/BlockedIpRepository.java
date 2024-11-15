package org.example.repository;

import org.example.entity.BlockedIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockedIpRepository extends JpaRepository<BlockedIp,Long> {

    BlockedIp getBlockedIpById(int id);

    boolean existsBlockedIpByIp(int ip);

    List<BlockedIp> findByIdGreaterThan(long id);

    BlockedIp findTopByOrderByIdDesc();


}
