package com.handy.appserver.entity.like;
import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "likes",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "target_id", "target_type"})})
@Getter
@Setter
@NoArgsConstructor
public class Like extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 좋아요 누른 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 대상 ID (SnapPost ID or User ID)
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private LikeTargetType targetType;
}
