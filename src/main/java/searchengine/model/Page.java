package searchengine.model;

import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Index;

@Data
@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList = "path"))
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;


    @Column(nullable = false)
    private Integer code;

    @Column(nullable = false)
    private String content;
}
