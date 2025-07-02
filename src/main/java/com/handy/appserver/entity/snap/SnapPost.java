package com.handy.appserver.entity.snap;

import com.handy.appserver.entity.comment.Comment;
import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "snap_posts")
@Getter
@Setter
public class SnapPost extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToMany(mappedBy = "snapPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SnapImage> images = new ArrayList<>();
    
    @OneToMany(mappedBy = "snapPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SnapLike> likes = new ArrayList<>();
    
    @OneToMany(mappedBy = "snapPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> comments = new ArrayList<>();
    
    @Column(nullable = false)
    private boolean isActive = true;
}

