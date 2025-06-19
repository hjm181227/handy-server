package com.handy.appserver.entity.snap;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
public class SnapImage {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snap_post_id")
    @Setter
    private SnapPost snapPost;
}
