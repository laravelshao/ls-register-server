package com.cl.register.server.cluster;

import com.cl.register.server.web.AbstractRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 集群同步 batch
 *
 * @author qinghua.shao
 * @date 2022/7/23
 * @since 1.0.0
 */
public class PeersReplicateBatch {

    private List<AbstractRequest> requests = new ArrayList<>();

    public void add(AbstractRequest request) {
        this.requests.add(request);
    }

    public List<AbstractRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<AbstractRequest> requests) {
        this.requests = requests;
    }

}