package com.example.checkspire.model.entity.tournament;

import com.example.checkspire.model.entity.common.BaseEntity;
import com.example.checkspire.model.enums.tournament.TournamentRoundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tournament_rounds",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "tournament_id",
                        "round_number"
                }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRound extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "tournament_id",
            nullable = false
    )
    private Tournament tournament;


    @Column(
            name = "round_number",
            nullable = false
    )
    private Integer roundNumber;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentRoundStatus status;


    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;


    @PrePersist
    private void onCreate() {

        if (status == null) {
            status =
                    TournamentRoundStatus.SCHEDULED;
        }
    }
}