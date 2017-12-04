// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package aca.com.remote.tunes.jmdns.impl.tasks.resolver;

import aca.com.remote.tunes.jmdns.impl.DNSEntry;
import aca.com.remote.tunes.jmdns.impl.DNSOutgoing;
import aca.com.remote.tunes.jmdns.impl.DNSQuestion;
import aca.com.remote.tunes.jmdns.impl.DNSRecord;
import aca.com.remote.tunes.jmdns.impl.JmDNSImpl;
import aca.com.remote.tunes.jmdns.impl.ServiceInfoImpl;
import aca.com.remote.tunes.jmdns.impl.constants.DNSRecordClass;
import aca.com.remote.tunes.jmdns.impl.constants.DNSRecordType;

import java.io.IOException;

/**
 * The ServiceInfoResolver queries up to three times consecutively for a service info, and then removes itself from the timer.
 * <p/>
 * The ServiceInfoResolver will run only if JmDNS is in state ANNOUNCED. REMIND: Prevent having multiple service resolvers for the same info in the timer queue.
 */
public class ServiceInfoResolver extends DNSResolverTask {

    private final ServiceInfoImpl _info;

    public ServiceInfoResolver(JmDNSImpl jmDNSImpl, ServiceInfoImpl info) {
        super(jmDNSImpl);
        this._info = info;
        info.setDns(this.getDns());
        this.getDns().addListener(info, DNSQuestion.newQuestion(info.getQualifiedName(), DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName() {
        return "ServiceInfoResolver(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#cancel()
     */
    @Override
    public boolean cancel() {
        // We should not forget to remove the listener
        boolean result = super.cancel();
        if (!_info.isPersistent()) {
            this.getDns().removeListener(_info);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addAnswers(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addAnswers(DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        if (!_info.hasData()) {
            long now = System.currentTimeMillis();
            newOut = this.addAnswer(newOut, (DNSRecord) this.getDns().getCache().getDNSEntry(_info.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN), now);
            newOut = this.addAnswer(newOut, (DNSRecord) this.getDns().getCache().getDNSEntry(_info.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN), now);
            if (_info.getServer().length() > 0) {
                for (DNSEntry addressEntry : this.getDns().getCache().getDNSEntryList(_info.getServer(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN)) {
                    newOut = this.addAnswer(newOut, (DNSRecord) addressEntry, now);
                }
                for (DNSEntry addressEntry : this.getDns().getCache().getDNSEntryList(_info.getServer(), DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_IN)) {
                    newOut = this.addAnswer(newOut, (DNSRecord) addressEntry, now);
                }
            }
        }
        return newOut;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addQuestions(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addQuestions(DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        if (!_info.hasData()) {
            newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(_info.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
            newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(_info.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
            if (_info.getServer().length() > 0) {
                newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(_info.getServer(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(_info.getServer(), DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
            }
        }
        return newOut;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#description()
     */
    @Override
    protected String description() {
        return "querying service info: " + (_info != null ? _info.getQualifiedName() : "null");
    }

}