package ahqpck.maintenance.report.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.entity.Equipment;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, String>, JpaSpecificationExecutor<Equipment> {

    Optional<Equipment> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);

    @Query("""
            SELECT e.id,
                   COUNT(DISTINCT wr.id) AS openWr,
                   COUNT(DISTINCT CASE WHEN wr.status = 'PENDING' THEN wr.id END) AS pendingWr,
                   COUNT(DISTINCT c.id) AS openC,
                   COUNT(DISTINCT CASE WHEN c.status = 'PENDING' THEN c.id END) AS pendingC
            FROM Equipment e
            LEFT JOIN WorkReport wr ON e.code = wr.equipment.code AND wr.status IN ('OPEN', 'PENDING')
            LEFT JOIN Complaint c ON e.code = c.equipment.code AND c.status IN ('OPEN', 'PENDING')
            WHERE e.id IN :equipmentIds
              AND e.id IS NOT NULL
            GROUP BY e.id
            """)
    List<EquipmentStats> findStatsByEquipmentIds(@Param("equipmentIds") List<String> equipmentIds);

    // Add this projection interface inside or near EquipmentRepository
    interface EquipmentStats {
        String getId();

        Long getOpenWr(); // OPEN work reports

        Long getPendingWr(); // PENDING work reports

        Long getOpenC(); // OPEN complaints

        Long getPendingC(); // PENDING complaints
    }
}