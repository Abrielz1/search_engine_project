package searchengine.model;

import lombok.Data;
import searchengine.model.Site;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "lemma ", indexes = @javax.persistence.Index(columnList = "lemma, site_id"))
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(nullable = false)
    private Integer frequency;
}
