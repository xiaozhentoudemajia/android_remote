// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package aca.com.remote.tunes.jmdns.impl.tasks.resolver;

import aca.com.remote.tunes.jmdns.impl.DNSOutgoing;
import aca.com.remote.tunes.jmdns.impl.DNSQuestion;
import aca.com.remote.tunes.jmdns.impl.DNSRecord;
import aca.com.remote.tunes.jmdns.impl.JmDNSImpl;
import aca.com.remote.tunes.jmdns.impl.JmDNSImpl.ServiceTypeEntry;
import aca.com.remote.tunes.jmdns.impl.constants.DNSConstants;
import aca.com.remote.tunes.jmdns.impl.constants.DNSRecordClass;
import aca.com.remote.tunes.jmdns.impl.constants.DNSRecordType;

import java.io.IOException;

/**
 * Helper class to resolve service types.
 * <p/>
 * The TypeResolver queries three times consecutively for service types, and then removes itself from the timer.
 * <p/>
 * The TypeResolver will run only if JmDNS is in state ANNOUNCED.
 */
public class TypeResolver extends DNSResolverTask {

    /**
     * @param jmDNSImpl
     */
    public TypeResolver(JmDNSImpl jmDNSImpl) {
        super(jmDNSImpl);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName() {
        return "TypeResolver(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addAnswers(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addAnswers(DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        long now = System.currentTimeMillis();
        for (String type : this.getDns().getServiceTypes().keySet()) {
            ServiceTypeEntry typeEntry = this.getDns().getServiceTypes().get(type);
            newOut = this.addAnswer(newOut, new DNSRecord.Pointer("_services._dns-sd._udp.local.", DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, typeEntry.getType()), now);
        }
        return newOut;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addQuestions(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addQuestions(DNSOutgoing out) throws IOException {
        return this.addQuestion(out, DNSQuestion.newQuestion("_services._dns-sd._udp.local.", DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#description()
     */
    @Override
    protected String description() {
        return "querying type";
    }
}