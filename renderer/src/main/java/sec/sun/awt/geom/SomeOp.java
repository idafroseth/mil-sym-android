/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sec.sun.awt.geom;

/**
 *
 * @author Michael Deutch
 */
public class SomeOp {
    public static final int SUBOP = 0;
    public static final int ADDOP = 1;
    public static final int INTOP = 2;
    public static final int XOROP = 3;
    
    public SomeOp(int type)
    {
        _type=type;
    }
    private int _type=-1;
        boolean inLeft;
        boolean inRight;
        boolean inResult;

        public void newRow() {
            inLeft = false;
            inRight = false;
            inResult = false;
        }

        public int classify(Edge e) {
            if (e.getCurveTag() == AreaOp2.CTAG_LEFT) {
                inLeft = !inLeft;
            } else {
                inRight = !inRight;
            }
            boolean newClass = newClassification(inLeft, inRight);
            if (inResult == newClass) {
                return AreaOp2.ETAG_IGNORE;
            }
            inResult = newClass;
            return (newClass ? AreaOp2.ETAG_ENTER : AreaOp2.ETAG_EXIT);
        }

        public int getState() {
            return (inResult ? AreaOp2.RSTAG_INSIDE : AreaOp2.RSTAG_OUTSIDE);
        }
    public boolean newClassification(boolean inLeft, boolean inRight) {
        switch(_type)
        {
            case ADDOP:
            return (inLeft || inRight);
            case SUBOP:
            return (inLeft && !inRight);
            case INTOP:
            return (inLeft && inRight);
            case XOROP:
            return (inLeft != inRight);
            default:
                return false;
                                
        }
    }
    
    
    //all the code from AreaOp that this class extends
    public Vector calculate(Vector left, Vector right) {
        Vector edges = new Vector();
        addEdges(edges, left, AreaOp2.CTAG_LEFT);
        addEdges(edges, right, AreaOp2.CTAG_RIGHT);
        edges = pruneEdges(edges);
        if (false) {
            System.out.println("result: ");
            int numcurves = edges.size();
            //Curve[] curvelist = (Curve[]) edges.toArray(new Curve[numcurves]);
            Curve[] curvelist = (Curve[]) edges.toArray2();
            for (int i = 0; i < numcurves; i++) {
                System.out.println("curvelist["+i+"] = "+curvelist[i]);
            }
        }
        return edges;
    }

    private static void addEdges(Vector edges, Vector curves, int curvetag) {
        Enumeration enum_ = curves.elements();
        CurveObject c=null;
        Object obj=null;
        while (enum_.hasMoreElements()) {
            //CurveObject c = (CurveObject) enum_.nextElement();
            obj=enum_.nextElement();
            if(obj instanceof CurveObject)
                c=(CurveObject)obj;
            else
                c=new CurveObject(obj);
            if (c.getOrder() > 0) {
                edges.add(new Edge(c, curvetag));
            }
        }
    }

    private Vector pruneEdges(Vector edges) {
        int numedges = edges.size();
        if (numedges < 2) {
            return edges;
        }
        //Edge[] edgelist = (Edge[]) edges.toArray(new Edge[numedges]);        
        //Edge[] edgelist = (Edge[]) edges.toArray();
        Edge[] edgelist = new Edge[numedges];
        Enumeration _enum=edges.elements();
        int k=0;
        while(_enum.hasMoreElements())
        {
            edgelist[k++]=(Edge)_enum.nextElement();
        }
        //Arrays.sort(edgelist, YXTopComparator);        
        Arrays.sort(edgelist);        
        if (false) {
            System.out.println("pruning: ");
            for (int i = 0; i < numedges; i++) {
                System.out.println("edgelist["+i+"] = "+edgelist[i]);
            }
        }
        Edge e;
        int left = 0;
        int right = 0;
        int cur = 0;
        int next = 0;
        double yrange[] = new double[2];
        Vector subcurves = new Vector();
        Vector chains = new Vector();
        Vector links = new Vector();
        // Active edges are between left (inclusive) and right (exclusive)
        while (left < numedges) {
            double y = yrange[0];
            // Prune active edges that fall off the top of the active y range
            for (cur = next = right - 1; cur >= left; cur--) {
                e = edgelist[cur];
                if (e.getCurve().getYBot() > y) {
                    if (next > cur) {
                        edgelist[next] = e;
                    }
                    next--;
                }
            }
            left = next + 1;
            // Grab a new "top of Y range" if the active edges are empty
            if (left >= right) {
                if (right >= numedges) {
                    break;
                }
                y = edgelist[right].getCurve().getYTop();
                if (y > yrange[0]) {
                    finalizeSubCurves(subcurves, chains);
                }
                yrange[0] = y;
            }
            // Incorporate new active edges that enter the active y range
            while (right < numedges) {
                e = edgelist[right];
                if (e.getCurve().getYTop() > y) {
                    break;
                }
                right++;
            }
            // Sort the current active edges by their X values and
            // determine the maximum valid Y range where the X ordering
            // is correct
            yrange[1] = edgelist[left].getCurve().getYBot();
            if (right < numedges) {
                y = edgelist[right].getCurve().getYTop();
                if (yrange[1] > y) {
                    yrange[1] = y;
                }
            }
            if (false) {
                System.out.println("current line: y = ["+
                                   yrange[0]+", "+yrange[1]+"]");
                for (cur = left; cur < right; cur++) {
                    System.out.println("  "+edgelist[cur]);
                }
            }
            // Note: We could start at left+1, but we need to make
            // sure that edgelist[left] has its equivalence set to 0.
            int nexteq = 1;
            for (cur = left; cur < right; cur++) {
                e = edgelist[cur];
                e.setEquivalence(0);
                for (next = cur; next > left; next--) {
                    Edge prevedge = edgelist[next-1];
                    int ordering = e.compareTo(prevedge, yrange);
                    if (yrange[1] <= yrange[0]) {
                        throw new InternalError("backstepping to "+yrange[1]+
                                                " from "+yrange[0]);
                    }
                    if (ordering >= 0) {
                        if (ordering == 0) {
                            // If the curves are equal, mark them to be
                            // deleted later if they cancel each other
                            // out so that we avoid having extraneous
                            // curve segments.
                            int eq = prevedge.getEquivalence();
                            if (eq == 0) {
                                eq = nexteq++;
                                prevedge.setEquivalence(eq);
                            }
                            e.setEquivalence(eq);
                        }
                        break;
                    }
                    edgelist[next] = prevedge;
                }
                edgelist[next] = e;                
            }
            if (false) {
                System.out.println("current sorted line: y = ["+
                                   yrange[0]+", "+yrange[1]+"]");
                for (cur = left; cur < right; cur++) {
                    System.out.println("  "+edgelist[cur]);
                }
            }
            // Now prune the active edge list.
            // For each edge in the list, determine its classification
            // (entering shape, exiting shape, ignore - no change) and
            // record the current Y range and its classification in the
            // Edge object for use later in constructing the new outline.
            newRow();
            double ystart = yrange[0];
            double yend = yrange[1];
            for (cur = left; cur < right; cur++) {
                e = edgelist[cur];
                int etag;
                int eq = e.getEquivalence();
                if (eq != 0) {
                    // Find one of the segments in the "equal" range
                    // with the right transition state and prefer an
                    // edge that was either active up until ystart
                    // or the edge that extends the furthest downward
                    // (i.e. has the most potential for continuation)
                    int origstate = getState();
                    etag = (origstate == AreaOp2.RSTAG_INSIDE
                            ? AreaOp2.ETAG_EXIT
                            : AreaOp2.ETAG_ENTER);
                    Edge activematch = null;
                    Edge longestmatch = e;
                    double furthesty = yend;
                    do {
                        // Note: classify() must be called
                        // on every edge we consume here.
                        classify(e);
                        if (activematch == null &&
                            e.isActiveFor(ystart, etag))
                        {
                            activematch = e;
                        }
                        y = e.getCurve().getYBot();
                        if (y > furthesty) {
                            longestmatch = e;
                            furthesty = y;
                        }
                    } while (++cur < right &&
                             (e = edgelist[cur]).getEquivalence() == eq);
                    --cur;
                    if (getState() == origstate) {
                        etag = AreaOp2.ETAG_IGNORE;
                    } else {
                        e = (activematch != null ? activematch : longestmatch);
                    }
                } else {
                    etag = classify(e);
                }
                if (etag != AreaOp2.ETAG_IGNORE) {
                    e.record(yend, etag);
                    links.add(new CurveLink(e.getCurve(), ystart, yend, etag));
                }
            }
            // assert(getState() == AreaOp.RSTAG_OUTSIDE);
            if (getState() != AreaOp2.RSTAG_OUTSIDE) {
                System.out.println("Still inside at end of active edge list!");
                System.out.println("num curves = "+(right-left));
                System.out.println("num links = "+links.size());
                System.out.println("y top = "+yrange[0]);
                if (right < numedges) {
                    System.out.println("y top of next curve = "+
                                       edgelist[right].getCurve().getYTop());
                } else {
                    System.out.println("no more curves");
                }
                for (cur = left; cur < right; cur++) {
                    e = edgelist[cur];
                    System.out.println(e);
                    int eq = e.getEquivalence();
                    if (eq != 0) {
                        System.out.println("  was equal to "+eq+"...");
                    }
                }
            }
            if (false) {
                System.out.println("new links:");
                for (int i = 0; i < links.size(); i++) {
                    CurveLink link = (CurveLink) links.elementAt(i);
                    System.out.println("  "+link.getSubCurve());
                }
            }
            resolveLinks(subcurves, chains, links);
            links.clear();
            // Finally capture the bottom of the valid Y range as the top
            // of the next Y range.
            yrange[0] = yend;
        }
        finalizeSubCurves(subcurves, chains);
        Vector ret = new Vector();
        Enumeration enum_ = subcurves.elements();
        CurveObject c=null;
        Object obj=null;
        while (enum_.hasMoreElements()) {
            CurveLink link = (CurveLink) enum_.nextElement();
            ret.add(link.getMoveto());
            CurveLink nextlink = link;
            while ((nextlink = nextlink.getNext()) != null) {
                if (!link.absorb(nextlink)) 
                {
                    //ret.add(link.getSubCurve());
                    obj=link.getSubCurve();
//                    if(! (obj instanceof CurveObject) )
//                    {
//                        CurveObject c=new CurveObject(obj);
//                        ret.add(c);
//                    }
//                    else
//                        ret.add(link.getSubCurve());
                    if(obj instanceof Order0)
                        c=((Order0)obj).getParent();
                    else if(obj instanceof Order1)
                        c=((Order1)obj).getParent();
                    else if(obj instanceof Order2)
                        c=((Order2)obj).getParent();
                    else if(obj instanceof Order3)
                        c=((Order3)obj).getParent();
                    else if(obj instanceof CurveObject)
                        c=(CurveObject)obj;
                    if(c==null)                    
                        c=new CurveObject(obj);
                    
                    
                    ret.add(c);
                    link = nextlink;
                }
            }
            obj=link.getSubCurve();
            //ret.add(link.getSubCurve());
            if(obj instanceof Order0)
                c=((Order0)obj).getParent();
            else if(obj instanceof Order1)
                c=((Order1)obj).getParent();
            else if(obj instanceof Order2)
                c=((Order2)obj).getParent();
            else if(obj instanceof Order3)
                c=((Order3)obj).getParent();
            else if(obj instanceof CurveObject)
                c=(CurveObject)obj;
            if(c==null)                    
                c=new CurveObject(obj);
            
            ret.add(c);
        }
        return ret;
    }

    public static void finalizeSubCurves(Vector subcurves, Vector chains) {
        int numchains = chains.size();
        if (numchains == 0) {
            return;
        }
        if ((numchains & 1) != 0) {
            throw new InternalError("Odd number of chains!");
        }
        ChainEnd[] endlist = new ChainEnd[numchains];
        chains.toArray(endlist);
        for (int i = 1; i < numchains; i += 2) {
            ChainEnd open = endlist[i - 1];
            ChainEnd close = endlist[i];
            CurveLink subcurve = open.linkTo(close);
            if (subcurve != null) {
                subcurves.add(subcurve);
            }
        }
        chains.clear();
    }

    private static CurveLink[] EmptyLinkList = new CurveLink[2];
    private static ChainEnd[] EmptyChainList = new ChainEnd[2];

    public static void resolveLinks(Vector subcurves,
                                    Vector chains,
                                    Vector links)
    {
        int numlinks = links.size();
        CurveLink[] linklist;
        if (numlinks == 0) {
            linklist = EmptyLinkList;
        } else {
            if ((numlinks & 1) != 0) {
                throw new InternalError("Odd number of new curves!");
            }
            linklist = new CurveLink[numlinks+2];
            links.toArray(linklist);
        }
        int numchains = chains.size();
        ChainEnd[] endlist;
        if (numchains == 0) {
            endlist = EmptyChainList;
        } else {
            if ((numchains & 1) != 0) {
                throw new InternalError("Odd number of chains!");
            }
            endlist = new ChainEnd[numchains+2];
            chains.toArray(endlist);
        }
        int curchain = 0;
        int curlink = 0;
        chains.clear();
        ChainEnd chain = endlist[0];
        ChainEnd nextchain = endlist[1];
        CurveLink link = linklist[0];
        CurveLink nextlink = linklist[1];
        while (chain != null || link != null) {
            /*
             * Strategy 1:
             * Connect chains or links if they are the only things left...
             */
            boolean connectchains = (link == null);
            boolean connectlinks = (chain == null);

            if (!connectchains && !connectlinks) {
                // assert(link != null && chain != null);
                /*
                 * Strategy 2:
                 * Connect chains or links if they close off an open area...
                 */
                connectchains = ((curchain & 1) == 0 &&
                                 chain.getX() == nextchain.getX());
                connectlinks = ((curlink & 1) == 0 &&
                                link.getX() == nextlink.getX());

                if (!connectchains && !connectlinks) {
                    /*
                     * Strategy 3:
                     * Connect chains or links if their successor is
                     * between them and their potential connectee...
                     */
                    double cx = chain.getX();
                    double lx = link.getX();
                    connectchains =
                        (nextchain != null && cx < lx &&
                         obstructs(nextchain.getX(), lx, curchain));
                    connectlinks =
                        (nextlink != null && lx < cx &&
                         obstructs(nextlink.getX(), cx, curlink));
                }
            }
            if (connectchains) {
                CurveLink subcurve = chain.linkTo(nextchain);
                if (subcurve != null) {
                    subcurves.add(subcurve);
                }
                curchain += 2;
                chain = endlist[curchain];
                nextchain = endlist[curchain+1];
            }
            if (connectlinks) {
                ChainEnd openend = new ChainEnd(link, null);
                ChainEnd closeend = new ChainEnd(nextlink, openend);
                openend.setOtherEnd(closeend);
                chains.add(openend);
                chains.add(closeend);
                curlink += 2;
                link = linklist[curlink];
                nextlink = linklist[curlink+1];
            }
            if (!connectchains && !connectlinks) {
                // assert(link != null);
                // assert(chain != null);
                // assert(chain.getEtag() == link.getEtag());
                chain.addLink(link);
                chains.add(chain);
                curchain++;
                chain = nextchain;
                nextchain = endlist[curchain+1];
                curlink++;
                link = nextlink;
                nextlink = linklist[curlink+1];
            }
        }
        if ((chains.size() & 1) != 0) {
            System.out.println("Odd number of chains!");
        }
    }

    /*
     * Does the position of the next edge at v1 "obstruct" the
     * connectivity between current edge and the potential
     * partner edge which is positioned at v2?
     *
     * Phase tells us whether we are testing for a transition
     * into or out of the interior part of the resulting area.
     *
     * Require 4-connected continuity if this edge and the partner
     * edge are both "entering into" type edges
     * Allow 8-connected continuity for "exiting from" type edges
     */
    public static boolean obstructs(double v1, double v2, int phase) {
        return (((phase & 1) == 0) ? (v1 <= v2) : (v1 < v2));
    }
   
}
