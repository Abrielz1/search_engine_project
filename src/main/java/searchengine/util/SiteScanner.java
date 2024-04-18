package searchengine.util;

import lombok.Getter;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

@Getter
public class SiteScanner {


    
 //   ExecutorService executor;   extends RecursiveTask<Void>



    public void scan() { // fork join


    }


   // @Override
    protected Void compute() {
        return null;
    }


}
