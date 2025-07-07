package com.handy.appserver.entity.snap;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "snap_likes")
@Getter
@NoArgsConstructor
public class SnapLike extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snap_post_id", nullable = false)
    private SnapPost snapPost;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    public SnapLike(SnapPost snapPost, User user) {
        this.snapPost = snapPost;
        this.user = user;
    }
} 