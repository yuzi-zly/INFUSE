package com.CC.Constraints.Runtime;

import java.util.HashSet;
import java.util.Set;

public class LGUtils {

    public void flip(Link link){
        link.setLinkType(link.getLinkType() == Link.Link_Type.SATISFIED ?
                Link.Link_Type.VIOLATED : Link.Link_Type.SATISFIED);
    }

    public Set<Link> flipSet(Set<Link> linkSet){
        Set<Link> result = new HashSet<>();
        for(Link link : linkSet){
            Link newLink = new Link(link.getLinkType() == Link.Link_Type.SATISFIED ? Link.Link_Type.VIOLATED : Link.Link_Type.SATISFIED);
            newLink.setVaSet(link.getVaSet());
            result.add(newLink);
        }
        return result;
    }

    public Link cartesian(Link link1, Link link2) {
        if (link1.getLinkType() != link2.getLinkType()){
            System.out.println("LGUtils: [Cartesian] The link type of two links are different.");
            System.out.println(link1);
            System.out.println(link2);
            System.exit(1);
            return null;
        }
        else{
            Link link = new Link(link1.getLinkType());
            link.getVaSet().addAll(link1.getVaSet());
            link.getVaSet().addAll(link2.getVaSet());
            return link;
        }
    }

    public Set<Link> cartesianSet(Set<Link> linkSet1, Set<Link> linkSet2) {
        Set<Link> result = new HashSet<>();
        if(linkSet1.isEmpty()){
            result.addAll(linkSet2);
            return result;
        }
        else if(linkSet2.isEmpty()){
            result.addAll(linkSet1);
            return result;
        }
        else{
            for(Link link1 : linkSet1){
                for(Link link2 : linkSet2){
                    result.add(cartesian(link1, link2));
                }
            }
            return result;
        }
    }

    public Set<Link> cloneSet(Set<Link> linkSet){
        Set<Link> result = new HashSet<>();
        for(Link link : linkSet){
            try {
                result.add(link.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
