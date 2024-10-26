package org.activebpel.rt.bpel.impl;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.xml.namespace.QName;
import org.activebpel.rt.AeException;
import org.activebpel.rt.IAePolicyConstants;
import org.activebpel.rt.attachment.IAeAttachmentItem;
import org.activebpel.rt.bpel.AeBusinessProcessException;
import org.activebpel.rt.bpel.AeMessages;
import org.activebpel.rt.bpel.AeWSDLDefHelper;
import org.activebpel.rt.bpel.IAeBusinessProcess;
import org.activebpel.rt.bpel.IAeBusinessProcessEngine;
import org.activebpel.rt.bpel.IAeEndpointReference;
import org.activebpel.rt.bpel.IAeEngineAlert;
import org.activebpel.rt.bpel.IAeEngineEvent;
import org.activebpel.rt.bpel.IAeEngineListener;
import org.activebpel.rt.bpel.IAeFault;
import org.activebpel.rt.bpel.IAeInvokeActivity;
import org.activebpel.rt.bpel.IAeMonitorListener;
import org.activebpel.rt.bpel.IAePlanManager;
import org.activebpel.rt.bpel.IAeProcessEvent;
import org.activebpel.rt.bpel.IAeProcessInfoEvent;
import org.activebpel.rt.bpel.IAeProcessListener;
import org.activebpel.rt.bpel.config.IAeEngineConfiguration;
import org.activebpel.rt.bpel.coord.IAeCoordinationContext;
import org.activebpel.rt.bpel.def.AePartnerLinkDef;
import org.activebpel.rt.bpel.def.AePartnerLinkOpKey;
import org.activebpel.rt.bpel.impl.list.AeProcessInstanceDetail;
import org.activebpel.rt.bpel.impl.queue.AeInboundReceive;
import org.activebpel.rt.bpel.impl.queue.AeMessageReceiver;
import org.activebpel.rt.bpel.impl.queue.AeReply;
import org.activebpel.rt.bpel.impl.reply.AeWaitableReplyReceiver;
import org.activebpel.rt.bpel.impl.reply.IAeReplyReceiver;
import org.activebpel.rt.bpel.impl.reply.IAeTransmissionTracker;
import org.activebpel.rt.bpel.urn.AeURNResolver;
import org.activebpel.rt.bpel.urn.IAeURNResolver;
import org.activebpel.rt.bpel.xpath.AeXPathHelper;
import org.activebpel.rt.message.AeMessagePartsMap;
import org.activebpel.rt.message.IAeMessageData;
import org.activebpel.rt.util.AeLongMap;
import org.activebpel.rt.util.AeSafelyViewableCollection;
import org.activebpel.rt.util.AeUtil;
import org.activebpel.rt.wsdl.def.IAeProperty;
import org.activebpel.rt.wsdl.def.IAePropertyAlias;
import org.activebpel.rt.xml.schema.AeTypeMapping;
import org.activebpel.wsio.AeWebServiceAttachment;
import org.activebpel.wsio.IAeMessageAcknowledgeCallback;
import org.activebpel.wsio.IAeWebServiceMessageData;
import org.activebpel.wsio.IAeWebServiceResponse;
import org.activebpel.wsio.invoke.AeInvokeResponse;
import org.activebpel.wsio.receive.AeMessageContext;
import org.activebpel.wsio.receive.IAeMessageContext;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/** The class implementing the business process execution engine. */
public class AeBusinessProcessEngine implements IAeBusinessProcessEngineInternal, IAeBusinessProcessEngineCallback {

    /**
    * Default engine id.
    */
    private static final int DEFAULT_ENGINE_ID = 1;

    private IAeURNResolver mURNResolver;

    /** The monitor status which indicates the current health of the system */
    protected int mMonitorStatus = IAeBusinessProcessEngine.MONITOR_NORMAL;

    /** Engine listeners */
    protected Collection mEngineListeners = new AeSafelyViewableCollection(new LinkedHashSet());

    /** Monitor listeners */
    protected Collection mMonitorListeners = new AeSafelyViewableCollection(new LinkedHashSet());

    /** Global process listeners */
    protected Collection mGlobalProcessListeners = new AeSafelyViewableCollection(new LinkedHashSet());

    /** Map of listeners, keyed by the process ID */
    protected HashMap mProcessListeners = new HashMap();

    /** The queue for incoming data that gets dispatched to processes */
    protected IAeQueueManager mQueueManager;

    /** Process manager */
    private IAeProcessManager mProcessManager;

    /** The lock manager for handling acquiring and releasing locks. */
    protected IAeLockManager mLockManager;

    /** The attachment manager for handling attachments to messages and variables. */
    protected IAeAttachmentManager mAttachmentManager;

    /** Maps process QName to its process plan */
    protected IAePlanManager mPlanManager;

    /** The type mapping to use when converting from schema to java and back. */
    private AeTypeMapping mTypeMapping = new AeTypeMapping();

    /** The engine configuration supplied during engine construction. */
    protected IAeEngineConfiguration mEngineConfiguration;

    /** The date the engine started */
    protected Date mStarted;

    /** Strategy for managing partner links. */
    private IAeEnginePartnerLinkStrategy mPartnerLinkStrategy;

    /** Coordination manager */
    private IAeCoordinationManagerInternal mCoordinationManager;

    /** Transmit and receive id manager. */
    private IAeTransmissionTracker mTransmissionTracker;

    /**
    * Process coordination handler.
    */
    protected IAeProcessCoordination mProcessCoordination;

    /**
    * Map containing process journal ids keyed by the process id.
    */
    protected AeLongMap mProcessJournalIdMap;

    /** Map from custom manager name to IAeManager. */
    private Map mCustomManagers = new HashMap();

    /**
    * Constructs a new engine with the passed configuration, queue manager,
    * process manager, and alarm manager.
    *
    * @param aEngineConfiguration The engine configuration to use for this engine.
    * @param aQueueManager The queue manager to be associated with this engine.
    * @param aProcessManager The process manager to be associated with this engine.
    * @param aLockManager The lock manager to be associated with this engine.
    * @param aAttachmentManager The attachment manager to be associated with this engine.
    */
    public AeBusinessProcessEngine(IAeEngineConfiguration aEngineConfiguration, IAeQueueManager aQueueManager, IAeProcessManager aProcessManager, IAeLockManager aLockManager, IAeAttachmentManager aAttachmentManager) {
        mEngineConfiguration = aEngineConfiguration;
        mQueueManager = aQueueManager;
        mProcessManager = aProcessManager;
        mLockManager = aLockManager;
        mAttachmentManager = aAttachmentManager;
        if (mQueueManager != null) mQueueManager.setEngine(this);
        if (mProcessManager != null) mProcessManager.setEngine(this);
        if (mLockManager != null) mLockManager.setEngine(this);
        if (mAttachmentManager != null) mAttachmentManager.setEngine(this);
        mStarted = new Date();
        mProcessJournalIdMap = new AeLongMap(Collections.synchronizedMap(new HashMap()));
    }

    /**
    * Adds the given process id and its journal id to an internal collection.
    * @param aProcessId
    * @param aJournalId
    */
    protected void addProcessJournalId(long aProcessId, long aJournalId) {
        mProcessJournalIdMap.put(aProcessId, new Long(aJournalId));
    }

    /**
    * Removes and returns the journal id for the given process id. If the journal id is not
    * found, then <code>IAeProcessManager.NULL_JOURNAL_ID</code> is returned.
    * @param aProcessId
    * @return journal id.
    */
    protected long removeProcessJournalId(long aProcessId) {
        Long journalIdObj = (Long) mProcessJournalIdMap.remove(aProcessId);
        if (journalIdObj != null) {
            return journalIdObj.longValue();
        } else {
            return IAeProcessManager.NULL_JOURNAL_ID;
        }
    }

    /**
    * Overrides method to return 1 as the default engine id.
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getEngineId()
    */
    public int getEngineId() {
        return DEFAULT_ENGINE_ID;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getProcessManager()
    */
    public IAeProcessManager getProcessManager() {
        return mProcessManager;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getTransmissionTracker()
    */
    public IAeTransmissionTracker getTransmissionTracker() {
        return mTransmissionTracker;
    }

    /**
    * Sets the transmission id tracker.
    * @param aTransmissionTracker
    */
    public void setTransmissionTracker(IAeTransmissionTracker aTransmissionTracker) {
        mTransmissionTracker = aTransmissionTracker;
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#getPartnerRoleEndpointReference(long, java.lang.String)
    */
    public IAeEndpointReference getPartnerRoleEndpointReference(long aPid, String aPartnerLinkPath) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessByIdNoUpdate(aPid);
        try {
            return process.getPartnerRoleEndpointReference(aPartnerLinkPath);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * Executes a business process
    * @param aPid - the pid for the process to execute
    * @param aExecuteImmediatelyFlag - a hint regarding whether a reply is waiting for the process to execute or not.
    */
    protected void executeProcess(long aPid, boolean aExecuteImmediatelyFlag) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.queueProcessToExecute();
            long journalId = removeProcessJournalId(aPid);
            if (journalId != IAeProcessManager.NULL_JOURNAL_ID) {
                if (getEngineConfiguration().isProcessRestartEnabled()) {
                    getProcessManager().journalEntryForRestart(aPid, journalId);
                } else {
                    getProcessManager().journalEntryDone(aPid, journalId);
                }
            }
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * Returns the business process with the specified process id, locking the
    * process into memory. <em>Each call to {@link #getProcessById(long)} must
    * be followed eventually by a matching call to
    * {@link #releaseProcess(IAeBusinessProcess)}</em>.
    *
    * @param aId
    */
    protected IAeBusinessProcess getProcessById(long aId) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessManager().getProcess(aId);
        if (process == null) {
            throw new AeBusinessProcessException(AeMessages.format("AeBusinessProcessEngine.ERROR_0", aId));
        }
        return process;
    }

    /**
    * Returns the business process with the specified process id, locking the
    * process into memory. <em>Each call to
    * {@link #getProcessByIdNoUpdate(long)} must be followed eventually by a
    * matching call to {@link #releaseProcess(IAeBusinessProcess)}</em>. Calling
    * {@link #getProcessByIdNoUpdate(long)} instead of
    * {@link #getProcessById(long)} asserts that the caller will not modify the
    * process, so that the process manager may discard the process from memory
    * after it is released without updating its persistent representation (if
    * there is one).
    *
    * @param aId
    */
    protected IAeBusinessProcess getProcessByIdNoUpdate(long aId) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessManager().getProcessNoUpdate(aId);
        if (process == null) {
            throw new AeBusinessProcessException(AeMessages.format("AeBusinessProcessEngine.ERROR_0", aId));
        }
        return process;
    }

    /**
    * Releases a process locked by {@link #getProcessById(long)} or
    * {@link #getProcessByIdNoUpdate(long)}.
    */
    protected void releaseProcess(IAeBusinessProcess aProcess) {
        getProcessManager().releaseProcess(aProcess);
    }

    /**
    * Convenience method that calls {@link
    * IAeProcessManager#journalEntryDone(long, long)}.
    */
    protected void journalEntryDone(IAeBusinessProcess aProcess, long aJournalId) {
        if (aProcess != null) {
            getProcessManager().journalEntryDone(aProcess.getProcessId(), aJournalId);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#setVariableData(long, java.lang.String, org.w3c.dom.Document)
    */
    public void setVariableData(long aPid, String aVariablePath, Document aVariableData) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.setVariableData(aVariablePath, aVariableData, true);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    */
    public IAeAttachmentItem addVariableAttachment(long aPid, String aVariablePath, AeWebServiceAttachment aAttachment) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            return process.addVariableAttachment(aVariablePath, aAttachment);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#removeVariableAttachments(long, java.lang.String, int[])
    */
    public void removeVariableAttachments(long aPid, String aVariablePath, int[] aAttachmentItemNumbers) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.removeVariableAttachments(aVariablePath, aAttachmentItemNumbers);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#getCorrelationData(long, java.lang.String)
    */
    public Map getCorrelationData(long aPid, String aCorrsetPath) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessByIdNoUpdate(aPid);
        try {
            return process.getCorrelationData(aCorrsetPath);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#setCorrelationData(long, java.lang.String, java.util.Map)
    */
    public void setCorrelationData(long aPid, String aCorrsetPath, Map aCorrelationData) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.setCorrelationData(aCorrsetPath, aCorrelationData);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#setPartnerLinkData(long, boolean, java.lang.String, org.w3c.dom.Document)
    */
    public void setPartnerLinkData(long aPid, boolean aIsPartnerRole, String aPartnerLinkPath, Document aPartnerEndpointRef) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.setPartnerLinkData(aIsPartnerRole, aPartnerLinkPath, aPartnerEndpointRef);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#suspendProcess(long)
    */
    public void suspendProcess(long aPid) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.suspend(null);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#resumeProcess(long)
    */
    public void resumeProcess(long aPid) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.resume(true);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#retryActivity(long, java.lang.String, boolean)
    */
    public void retryActivity(long aPid, String aLocation, boolean aAtScope) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.retryActivity(aLocation, aAtScope);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#completeActivity(long, java.lang.String)
    */
    public void completeActivity(long aPid, String aLocation) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.completeActivity(aLocation);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#terminateProcess(long)
    */
    public void terminateProcess(long aPid) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.terminate();
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#resumeProcessObject(long, java.lang.String)
    */
    public void resumeProcessObject(long aPid, String aLocation) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aPid);
        try {
            process.resume(aLocation);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#queueReceiveData(org.activebpel.wsio.receive.IAeMessageContext, org.activebpel.wsio.IAeWebServiceMessageData)
    */
    public IAeWebServiceResponse queueReceiveData(IAeMessageContext aContext, IAeWebServiceMessageData aData) throws AeBusinessProcessException {
        IAeReceiveHandler handler = getQueueManager().getReceiveHandler(aContext.getReceiveHandler());
        return handler.handleReceiveData(aData, aContext);
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#queueReceiveData(org.activebpel.wsio.receive.IAeMessageContext, org.w3c.dom.Document[])
    */
    public IAeWebServiceResponse queueReceiveData(IAeMessageContext aContext, Document[] aDataArray) throws AeBusinessProcessException {
        IAeReceiveHandler handler = getQueueManager().getReceiveHandler(aContext.getReceiveHandler());
        return handler.handleReceiveData(aDataArray, aContext);
    }

    /**
    * Returns the timeout in seconds for a web service timeout
    * @param aPlan the plan
    * @param aPartnerLink the partner link
    */
    protected int getWebServiceTimeout(IAeProcessPlan aPlan, String aPartnerLink) {
        return getEngineConfiguration().getWebServiceReceiveTimeout();
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#queueReceiveData(org.activebpel.rt.bpel.impl.IAeProcessPlan, org.activebpel.wsio.receive.IAeMessageContext, org.activebpel.rt.message.IAeMessageData)
    */
    public IAeWebServiceResponse queueReceiveData(IAeProcessPlan aPlan, IAeMessageContext aContext, IAeMessageData aData) throws AeBusinessProcessException {
        IAeProcessPlan plan = aPlan;
        if (plan == null) {
            plan = getProcessPlan(AeMessageContext.convert(aContext));
        }
        AePartnerLinkDef plDef = plan.getProcessDef().findPartnerLink(aContext.getPartnerLink());
        AePartnerLinkOpKey plOpKey = new AePartnerLinkOpKey(plDef, aContext.getOperation());
        boolean isOneWay = plan.getProcessDef().isOneWayReceive(plOpKey);
        AeWaitableReplyReceiver replyReceiver = isOneWay ? null : new AeWaitableReplyReceiver();
        AeInvokeResponse response = new AeInvokeResponse();
        long pid = IAeBusinessProcess.NULL_PROCESS_ID;
        try {
            pid = queueReceiveData(plan, aData, replyReceiver, aContext);
            if (replyReceiver != null) {
                synchronized (replyReceiver) {
                    if (replyReceiver.isWaitable()) {
                        try {
                            replyReceiver.wait(getWebServiceTimeout(aPlan, aContext.getPartnerLink()) * 1000);
                        } catch (InterruptedException e) {
                            throw new AeBusinessProcessException(AeMessages.getString("AeBusinessProcessEngine.WAIT_INTERRUPT"));
                        }
                    }
                }
                if (replyReceiver.getMessageData() != null) response.setMessageData(AeDataConverter.convert(replyReceiver.getMessageData())); else if (replyReceiver.getFault() != null) {
                    response.setFaultData(replyReceiver.getFault().getFaultName(), AeDataConverter.convert(replyReceiver.getFault().getMessageData()));
                    response.setErrorString(replyReceiver.getFault().getInfo());
                    response.setErrorDetail(replyReceiver.getFault().getDetailedInfo());
                } else {
                    String msg = AeMessages.format("AeBusinessProcessEngine.WARNING_TIMEOUT_WAITING_FOR_REPLY", new Object[] { aPlan.getProcessDef().getName(), String.valueOf(pid) });
                    throw new AeBpelException(msg, AeFaultFactory.getTimeoutFault(msg));
                }
                response.setBusinessProcessProperties(replyReceiver.getBusinessProcessProperties());
            }
        } finally {
            if (aData != null && aData.hasAttachments()) getAttachmentManager().responseFilled(pid);
        }
        return response;
    }

    /**
    * Gets the plan used to route the incoming message based on the information in the context.
    * At this layer, the plan is based on the process QName only. This will change with
    * other engines that support more advanced deployment options.
    *
    * @param aContext
    * @throws AeBusinessProcessException
    */
    public IAeProcessPlan getProcessPlan(AeMessageContext aContext) throws AeBusinessProcessException {
        return getProcessPlan(aContext.getProcessName());
    }

    /**
    * Gets the plan for the given process name.
    *
    * @param aProcessName
    */
    protected IAeProcessPlan getProcessPlan(QName aProcessName) throws AeBusinessProcessException {
        return getPlanManager().findCurrentPlan(aProcessName);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#queueReceiveData(org.activebpel.rt.message.IAeMessageData, org.activebpel.rt.bpel.impl.reply.IAeReplyReceiver, org.activebpel.wsio.receive.IAeMessageContext, org.activebpel.wsio.IAeMessageAcknowledgeCallback, boolean)
    */
    public long queueReceiveData(IAeMessageData aMessageData, IAeReplyReceiver aReply, IAeMessageContext aContext, IAeMessageAcknowledgeCallback aAckCallback, boolean aQueueForExecution) throws AeCorrelationViolationException, AeConflictingRequestException, AeBusinessProcessException {
        IAeProcessPlan processPlan = getProcessPlan(AeMessageContext.convert(aContext));
        AePartnerLinkOpKey plOpKey = getPartnerLinkOpKey(aContext, processPlan);
        Map correlationMap = createCorrelationMap(processPlan, aMessageData, aContext);
        AeInboundReceive inboundReceive = new AeInboundReceive(plOpKey, correlationMap, processPlan, aMessageData, aContext, aReply);
        return queueReceiveData(inboundReceive, aAckCallback, aQueueForExecution);
    }

    /**
    * Given a message context and plan returns the partner link Operation to perform.
    * @param aContext the message context
    * @param aPlan the target plan
    * @return the partnerlink operation key
    */
    private AePartnerLinkOpKey getPartnerLinkOpKey(IAeMessageContext aContext, IAeProcessPlan aPlan) {
        AePartnerLinkDef plDef = aPlan.getProcessDef().findPartnerLink(aContext.getPartnerLink());
        AePartnerLinkOpKey plOpKey = new AePartnerLinkOpKey(plDef, aContext.getOperation());
        return plOpKey;
    }

    /**
    * Queues a previously created process for execution.
    *
    * @param aProcessId child process id.
    * @throws AeBusinessProcessException
    */
    public void executeProcess(long aProcessId) throws AeBusinessProcessException {
        executeProcess(aProcessId, true);
    }

    /**
    * Gets the ack callback to use.
    *
    * @param aContext
    */
    protected IAeMessageAcknowledgeCallback getAcknowledgeCallback(IAeMessageContext aContext) {
        return null;
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#queueReceiveData(org.activebpel.rt.bpel.impl.IAeProcessPlan, org.activebpel.rt.message.IAeMessageData, org.activebpel.rt.bpel.impl.reply.IAeReplyReceiver, org.activebpel.wsio.receive.IAeMessageContext)
    */
    public long queueReceiveData(IAeProcessPlan aProcessPlan, IAeMessageData aMessageData, IAeReplyReceiver aReply, IAeMessageContext aContext) throws AeBusinessProcessException {
        IAeProcessPlan plan = aProcessPlan;
        if (plan == null) {
            plan = getProcessPlan(AeMessageContext.convert(aContext));
        }
        AePartnerLinkOpKey plOpKey = getPartnerLinkOpKey(aContext, plan);
        Map correlationMap = createCorrelationMap(plan, aMessageData, aContext);
        boolean isOneWay = plan.getProcessDef().isOneWayReceive(plOpKey);
        IAeReplyReceiver replyReceiver = aReply;
        if (isOneWay && replyReceiver != null) {
            replyReceiver.onMessage(null, null);
            replyReceiver = null;
        }
        AeInboundReceive inboundReceive = new AeInboundReceive(plOpKey, correlationMap, plan, aMessageData, aContext, replyReceiver);
        IAeMessageAcknowledgeCallback ackCallback = getAcknowledgeCallback(aContext);
        return queueReceiveData(inboundReceive, ackCallback, true);
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#queueReceiveData(org.activebpel.rt.bpel.impl.queue.AeInboundReceive, org.activebpel.wsio.IAeMessageAcknowledgeCallback, boolean)
    */
    public long queueReceiveData(AeInboundReceive aInboundReceive, IAeMessageAcknowledgeCallback aAckCallback, boolean aQueueForExecution) throws AeCorrelationViolationException, AeConflictingRequestException, AeBusinessProcessException {
        return queueReceiveDataInternal(aInboundReceive, aAckCallback, aQueueForExecution);
    }

    protected long queueReceiveDataInternal(AeInboundReceive aInboundReceive, IAeMessageAcknowledgeCallback aAckCallback, boolean aQueueForExecution) throws AeCorrelationViolationException, AeConflictingRequestException, AeBusinessProcessException {
        IAeProcessPlan plan = aInboundReceive.getProcessPlan();
        boolean canCreateInstance = plan.isCreateInstance(aInboundReceive.getPartnerLinkOperationKey());
        boolean createInstanceOnly = canCreateInstance && plan.getProcessDef().isCreateInstanceOperationOnly();
        boolean coordinated = isCoordinated(aInboundReceive.getContext());
        long processId = IAeBusinessProcess.NULL_PROCESS_ID;
        AeMessageReceiver found = null;
        IAeMessageData messageData = aInboundReceive.getMessageData();
        if (messageData != null && messageData.hasAttachments()) {
            getAttachmentManager().storeAttachments(messageData.getAttachmentContainer(), plan);
        }
        if (!coordinated && !createInstanceOnly) {
            found = getQueueManager().matchInboundReceive(aInboundReceive, !canCreateInstance, aAckCallback);
        }
        if (found != null) {
            processId = found.getProcessId();
            boolean synchronous = !aInboundReceive.isOneway();
            if (synchronous && messageData != null && messageData.hasAttachments()) getAttachmentManager().responsePending(processId);
            int locationId = found.getMessageReceiverPathId();
            long journalId = found.getJournalId();
            dispatchReceiveData(processId, locationId, aInboundReceive, journalId, synchronous);
        } else if (found == null && canCreateInstance) {
            processId = createProcessWithMessage(aInboundReceive, aAckCallback, aQueueForExecution);
        } else if (coordinated) {
            throw new AeCorrelationViolationException(aInboundReceive.getProcessPlan().getProcessDef().getNamespace(), AeCorrelationViolationException.NOT_CREATEINSTANCE);
        } else {
        }
        return processId;
    }

    /**
    * Dispatches the message data to the process's message receiver.
    *
    * @param aProcessId
    * @param aLocationId
    * @param aInboundReceive
    * @param aJournalId journal id of the message if it has been journalled (it may have already been) or {@link IAeProcessManager#NULL_JOURNAL_ID}
    * @param aRunSynchronously if true, indicates that the process should be run synchronously.
    */
    protected void dispatchReceiveData(long aProcessId, int aLocationId, AeInboundReceive aInboundReceive, long aJournalId, boolean aRunSynchronously) throws AeBusinessProcessException {
        internalDispatchReceiveData(aProcessId, aLocationId, aInboundReceive, aJournalId);
    }

    /**
    * Dispatches the message data to the process's message receiver.
    *
    * @param aProcessId
    * @param aLocationPathId
    * @param aInboundReceive
    * @param aJournalId journal id of the message if it has been journalled (it may have already been) or {@link IAeProcessManager#NULL_JOURNAL_ID}
    */
    protected void internalDispatchReceiveData(long aProcessId, int aLocationPathId, AeInboundReceive aInboundReceive, long aJournalId) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aProcessId);
        try {
            process.dispatchReceiveData(aLocationPathId, aInboundReceive, aJournalId);
            journalEntryDone(process, aJournalId);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * Creates the correlation map for the incoming receive.
    * @param aDesc
    * @param aData
    * @param aContext
    */
    private Map createCorrelationMap(IAeProcessPlan aDesc, IAeMessageData aData, IAeMessageContext aContext) throws AeBusinessProcessException {
        Map map = new HashMap();
        AePartnerLinkDef plDef = aDesc.getProcessDef().findPartnerLink(aContext.getPartnerLink());
        AePartnerLinkOpKey plOpKey = new AePartnerLinkOpKey(plDef, aContext.getOperation());
        AeMessagePartsMap messagePartsMap = aDesc.getProcessDef().getMessageForCorrelation(plOpKey);
        for (Iterator iter = aDesc.getCorrelatedPropertyNames(plOpKey).iterator(); iter.hasNext(); ) {
            QName propName = (QName) iter.next();
            IAePropertyAlias propAlias = aDesc.getProcessDef().getPropertyAliasForCorrelation(messagePartsMap, propName);
            IAeProperty prop = AeWSDLDefHelper.getProperty(aDesc, propName);
            if (prop == null) throw new AeBusinessProcessException(AeMessages.format("AeBusinessProcessEngine.ERROR_MISSING_PROPERTY", propName));
            map.put(propName, AeXPathHelper.getInstance(aDesc.getProcessDef().getNamespace()).extractCorrelationPropertyValue(propAlias, aData, getTypeMapping(), prop.getTypeName()));
        }
        String convId = aContext.getWsAddressingHeaders().getConversationId();
        if (AeUtil.notNullOrEmpty(convId)) {
            map.put(IAePolicyConstants.CONVERSATION_ID_HEADER, convId);
        }
        return map;
    }

    /**
    * @return Returns the coordination manager.
    */
    public IAeCoordinationManagerInternal getCoordinationManager() {
        return mCoordinationManager;
    }

    /**
    * Sets the coordination manager.
    * @param aCoordinationManager coordination manager.
    */
    public void setCoordinationManager(IAeCoordinationManagerInternal aCoordinationManager) {
        mCoordinationManager = aCoordinationManager;
        if (mCoordinationManager != null) {
            mCoordinationManager.setEngine(this);
        }
    }

    /**
    * Registers the given context for coordination with the coordination manager. Returns
    * true once the process is registered.
    * @param aContext message context
    * @param aProcess process
    * @throws AeBusinessProcessException
    */
    public boolean registerForCoordination(IAeMessageContext aContext, IAeBusinessProcess aProcess) throws AeBusinessProcessException {
        return false;
    }

    /**
    * Returns true if message context indicates that request should be coordinated.
    * @param aContext
    * @return returns true if the message context's business properties has a coordination context
    */
    public boolean isCoordinated(IAeMessageContext aContext) {
        return getCoordinationContext(aContext) != null;
    }

    /**
    * Returns the coordination context from the message context's business properties map.
    * @param aContext receive message context
    * @return coordination context if available or null otherwise.
    */
    protected IAeCoordinationContext getCoordinationContext(IAeMessageContext aContext) {
        return null;
    }

    /**
    * Creates a process and optionally queues it for execution. If the message acknowlege callback is not null,
    * then it will be invoked as part of create process transaction.
    *
    * @param aInboundReceive
    * @param aAckCallback
    * @param aQueueForExecution
    * @return process id.
    * @throws AeBusinessProcessException
    */
    protected long createProcessWithMessage(AeInboundReceive aInboundReceive, IAeMessageAcknowledgeCallback aAckCallback, boolean aQueueForExecution) throws AeBusinessProcessException {
        return createProcessWithMessage(aInboundReceive, aAckCallback, aQueueForExecution, IAeBusinessProcess.NULL_PROCESS_ID);
    }

    /**
    * Creates a process and optionally queues it for execution. If the message acknowlege callback is not null,
    * then it will be invoked as part of create process transaction.
    *
    * @param aInboundReceive
    * @param aAckCallback
    * @param aQueueForExecution
    * @param aRestartProcessId restart the given process if <code>aRestartProcessId</code> is not {@link IAeBusinessProcess#NULL_PROCESS_ID}
    * @return process id.
    * @throws AeBusinessProcessException
    */
    protected long createProcessWithMessage(AeInboundReceive aInboundReceive, IAeMessageAcknowledgeCallback aAckCallback, boolean aQueueForExecution, long aRestartProcessId) throws AeBusinessProcessException {
        IAeBusinessProcess process = internalCreateProcessWithMessage(aInboundReceive, aAckCallback, aRestartProcessId);
        try {
            long processId = process.getProcessId();
            if (!aInboundReceive.isOneway() && aInboundReceive.getMessageData().hasAttachments()) getAttachmentManager().responsePending(processId);
            if (aQueueForExecution) {
                executeProcess(processId, !aInboundReceive.isOneway());
            }
            return processId;
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * Create a process with inbound message.
    *
    * @param aInboundReceive
    * @param aAckCallback
    * @param aRestartProcessId restart the given process if <code>aRestartProcessId</code> is not {@link IAeBusinessProcess#NULL_PROCESS_ID}
    * @throws AeBusinessProcessException
    */
    protected IAeBusinessProcess internalCreateProcessWithMessage(AeInboundReceive aInboundReceive, IAeMessageAcknowledgeCallback aAckCallback, long aRestartProcessId) throws AeBusinessProcessException {
        IAeProcessPlan plan = aInboundReceive.getProcessPlan();
        IAeBusinessProcess process;
        if (aRestartProcessId == IAeBusinessProcess.NULL_PROCESS_ID) {
            process = getProcessManager().createBusinessProcess(plan);
        } else {
            if (plan.getProcessDef().isMultiStart()) {
                throw new AeBusinessProcessException(AeMessages.format("AeBusinessProcessEngine.ERROR_RestartMultistartProcess", aRestartProcessId));
            }
            process = getProcessManager().recreateBusinessProcess(aRestartProcessId, plan);
        }
        long processId = process.getProcessId();
        IAeMessageData messageData = aInboundReceive.getMessageData();
        if (messageData != null && messageData.hasAttachments()) getAttachmentManager().associateProcess(messageData.getAttachmentContainer(), processId);
        long journalId = getProcessManager().journalInboundReceive(processId, 0, aInboundReceive);
        long replyId = journalId;
        if (replyId == IAeProcessManager.NULL_JOURNAL_ID) {
            replyId = getProcessManager().getNextJournalId();
        }
        aInboundReceive.setReplyId(replyId);
        if (aRestartProcessId == IAeBusinessProcess.NULL_PROCESS_ID) {
            initializeCoordination(aInboundReceive, process);
            fireEngineEvent(new AeEngineEvent(processId, IAeEngineEvent.PROCESS_CREATED, aInboundReceive.getProcessName()));
        } else {
            if (isCoordinated(aInboundReceive.getContext())) {
                throw new AeBusinessProcessException(AeMessages.format("AeBusinessProcessEngine.ERROR_RestartCoordinatedInboundReceive", String.valueOf(aRestartProcessId)));
            }
            fireEngineEvent(new AeEngineEvent(processId, IAeEngineEvent.PROCESS_RECREATED, aInboundReceive.getProcessName()));
        }
        if (aInboundReceive.getReplyReceiver() != null && aInboundReceive.getReplyReceiver().getDurableReplyInfo() == null) {
            getQueueManager().addNonDurableReply(new AeReply(processId, replyId, aInboundReceive.getReplyReceiver()), null);
        }
        initializeProcess(aInboundReceive, process);
        if (aAckCallback != null) {
            try {
                aAckCallback.onAcknowledge(String.valueOf(processId));
            } catch (Throwable t) {
                throw new AeBusinessProcessException(t.getMessage(), t);
            }
        }
        addProcessJournalId(process.getProcessId(), journalId);
        return process;
    }

    /**
    * Initialize newly create process.
    * @param aInboundReceive
    * @param aProcess
    */
    protected void initializeProcess(AeInboundReceive aInboundReceive, IAeBusinessProcess aProcess) throws AeBusinessProcessException {
        aProcess.setCreateMessage(aInboundReceive);
        String plLocation = aProcess.getLocationPath(aInboundReceive.getPartnerLinkOperationKey().getPartnerLinkId());
        AePartnerLink plink = aProcess.findProcessPartnerLink(plLocation);
        getPartnerLinkStrategy().updatePartnerLink(plink, aProcess.getProcessPlan(), aInboundReceive.getContext());
    }

    /**
    * Check if the process under coordination, if so, register for coordnination.
    * @param aInboundReceive
    * @param aProcess
    */
    protected void initializeCoordination(AeInboundReceive aInboundReceive, IAeBusinessProcess aProcess) throws AeBusinessProcessException {
        if (isCoordinated(aInboundReceive.getContext())) {
            try {
                if (registerForCoordination(aInboundReceive.getContext(), aProcess)) {
                    aProcess.setParticipant(true);
                }
            } catch (Exception e) {
                AeException.logError(e, e.getMessage());
                aProcess.terminate();
                throw new AeBusinessProcessException(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#queueInvokeData(long, java.lang.String, long, org.activebpel.rt.message.IAeMessageData, java.util.Map)
    */
    public final void queueInvokeData(long aProcessId, String aLocationPath, long aTransmissionId, IAeMessageData aMessageData, Map aProperties) throws AeBusinessProcessException {
        queueInvokeData(aProcessId, aLocationPath, aTransmissionId, aMessageData, aProperties, null);
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#queueInvokeData(long, java.lang.String, long,  org.activebpel.rt.message.IAeMessageData, java.util.Map, org.activebpel.wsio.IAeMessageAcknowledgeCallback)
    */
    public void queueInvokeData(long aProcessId, String aLocationPath, long aTransmissionId, IAeMessageData aMessageData, Map aProperties, IAeMessageAcknowledgeCallback aAckCallback) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aProcessId);
        try {
            int locationId = process.getLocationId(aLocationPath);
            IAeInvokeActivity invoke = (IAeInvokeActivity) process.findBpelObject(locationId);
            if (matchesInvoke(aProcessId, aLocationPath, invoke, aTransmissionId)) {
                if (aMessageData != null && aMessageData.hasAttachments()) getAttachmentManager().storeAttachments(aMessageData.getAttachmentContainer(), process.getProcessPlan(), aProcessId);
                long journalId = journalQueueInvokeData(aProcessId, locationId, aTransmissionId, aMessageData, aProperties, aAckCallback);
                process.dispatchInvokeData(aLocationPath, aMessageData, aProperties);
                journalEntryDone(process, journalId);
            } else {
                handleUnmatchedInvoke(aProcessId, aLocationPath, invoke, aTransmissionId, aAckCallback);
            }
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * Journals the queue invoke data  and calls back on the optional message acknowlege interface.
    * @param aProcessId
    * @param aLocationId
    * @param aTransmissionId invoke activity's transmission id.
    * @param aMessageData
    * @param aProperties
    * @param aAckCallback
    * @throws AeBusinessProcessException
    */
    protected long journalQueueInvokeData(long aProcessId, int aLocationId, long aTransmissionId, IAeMessageData aMessageData, Map aProperties, IAeMessageAcknowledgeCallback aAckCallback) throws AeBusinessProcessException {
        long journalId = getProcessManager().journalInvokeData(aProcessId, aLocationId, aTransmissionId, aMessageData, aProperties);
        if (aAckCallback != null) {
            try {
                aAckCallback.onAcknowledge(null);
            } catch (Throwable t) {
                throw new AeBusinessProcessException(t.getMessage(), t);
            }
        }
        return journalId;
    }

    /**
    * Journals the queue invoke fault and calls back on the optional message acknowlege interface.
    * @param aProcessId
    * @param aLocationId
    * @param aTransmissionId invoke activity's transmission id.
    * @param aFault
    * @param aProperties
    * @param aAckCallback
    * @throws AeBusinessProcessException
    */
    protected long journalQueueInvokeFault(long aProcessId, int aLocationId, long aTransmissionId, IAeFault aFault, Map aProperties, IAeMessageAcknowledgeCallback aAckCallback) throws AeBusinessProcessException {
        long journalId = getProcessManager().journalInvokeFault(aProcessId, aLocationId, aTransmissionId, aFault, aProperties);
        if (aAckCallback != null) {
            try {
                aAckCallback.onAcknowledge(null);
            } catch (Throwable t) {
                throw new AeBusinessProcessException(t.getMessage(), t);
            }
        }
        return journalId;
    }

    /**
    * Calls the <code>onNotAcknowledge</code>. Normally, invoked when a message is not
    * consumed by a process.
    * @param aAckCallback
    * @param aReason
    * @throws AeBusinessProcessException
    */
    protected void nackCallback(IAeMessageAcknowledgeCallback aAckCallback, Throwable aReason) throws AeBusinessProcessException {
        if (aAckCallback != null) {
            try {
                aAckCallback.onNotAcknowledge(aReason);
            } catch (Throwable t) {
                throw new AeBusinessProcessException(t.getMessage(), t);
            }
        }
    }

    /**
    * Returns true of the invoke activity is currently executing and its tranmission id matches
    * the given transmission id.
    *
    * @param aProcessId
    * @param aLocationPath
    * @param aInvoke
    * @param aTransmissionId
    * @throws AeBusinessProcessException
    */
    protected boolean matchesInvoke(long aProcessId, String aLocationPath, IAeInvokeActivity aInvoke, long aTransmissionId) throws AeBusinessProcessException {
        if (aInvoke != null && aInvoke.isExecuting() && aInvoke.getTransmissionId() == aTransmissionId) {
            return true;
        } else {
            return false;
        }
    }

    /**
    * Handles unmatched invoke by throwing a business process exception based on the invoke's
    * current state and transmission id.
    * @param aProcessId
    * @param aLocationPath
    * @param aInvoke
    * @param aTransmissionId
    * @param aAckCallback
    * @throws AeBusinessProcessException
    */
    protected void handleUnmatchedInvoke(long aProcessId, String aLocationPath, IAeInvokeActivity aInvoke, long aTransmissionId, IAeMessageAcknowledgeCallback aAckCallback) throws AeBusinessProcessException {
        AeBusinessProcessException bpe = null;
        if (aInvoke != null && aInvoke.isExecuting() && aInvoke.getTransmissionId() != aTransmissionId) {
            Object[] args = { String.valueOf(aProcessId), aLocationPath, String.valueOf(aInvoke.getTransmissionId()), String.valueOf(aTransmissionId) };
            bpe = new AeBusinessProcessException(AeMessages.format("AeBusinessProcessEngine.UNMATCHED_INVOKE_ID", args));
        } else {
            Object[] args = { String.valueOf(aProcessId), aLocationPath };
            bpe = new AeBusinessProcessException(AeMessages.format("AeBusinessProcessEngine.UNMATCHED_INVOKE", args));
        }
        nackCallback(aAckCallback, bpe);
        throw bpe;
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#queueInvokeFault(long, java.lang.String, long, org.activebpel.rt.bpel.IAeFault, java.util.Map)
    */
    public final void queueInvokeFault(long aProcessId, String aLocationPath, long aTransmissionId, IAeFault aFault, Map aProperties) throws AeBusinessProcessException {
        queueInvokeFault(aProcessId, aLocationPath, aTransmissionId, aFault, aProperties, null);
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#queueInvokeFault(long, java.lang.String, long, org.activebpel.rt.bpel.IAeFault, java.util.Map, org.activebpel.wsio.IAeMessageAcknowledgeCallback)
    */
    public void queueInvokeFault(long aProcessId, String aLocationPath, long aTransmissionId, IAeFault aFault, Map aProperties, IAeMessageAcknowledgeCallback aAckCallback) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessById(aProcessId);
        long journalId = IAeProcessManager.NULL_JOURNAL_ID;
        try {
            int locationId = process.getLocationId(aLocationPath);
            IAeInvokeActivity invoke = (IAeInvokeActivity) process.findBpelObject(locationId);
            if (matchesInvoke(aProcessId, aLocationPath, invoke, aTransmissionId)) {
                IAeMessageData aMessageData = aFault.getMessageData();
                if (aMessageData != null && aMessageData.hasAttachments()) getAttachmentManager().storeAttachments(aMessageData.getAttachmentContainer(), process.getProcessPlan(), aProcessId);
                journalId = journalQueueInvokeFault(aProcessId, locationId, aTransmissionId, aFault, aProperties, aAckCallback);
                process.dispatchInvokeFault(aLocationPath, aFault, aProperties);
                journalEntryDone(process, journalId);
            } else {
                handleUnmatchedInvoke(aProcessId, aLocationPath, invoke, aTransmissionId, aAckCallback);
            }
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getQueueManager()
    */
    public IAeQueueManager getQueueManager() {
        return mQueueManager;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getLockManager()
    */
    public IAeLockManager getLockManager() {
        return mLockManager;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getAttachmentManager()
    */
    public IAeAttachmentManager getAttachmentManager() {
        return mAttachmentManager;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#setPlanManager(org.activebpel.rt.bpel.IAePlanManager)
    */
    public void setPlanManager(IAePlanManager aPlanManager) {
        mPlanManager = aPlanManager;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getPlanManager() 
    */
    public IAePlanManager getPlanManager() {
        return mPlanManager;
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#getEngineConfiguration()
    */
    public IAeEngineConfiguration getEngineConfiguration() {
        return mEngineConfiguration;
    }

    /**
    * Sets the configuration to use for this engine, note this is usually set during construction.
    * @param aConfiguration The configuration to use for this engine.
    */
    public void setEngineConfiguration(IAeEngineConfiguration aConfiguration) {
        mEngineConfiguration = aConfiguration;
    }

    /**
    * Overrides method to return self.
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getEngineCallback()
    */
    public IAeBusinessProcessEngineCallback getEngineCallback() {
        return this;
    }

    /**
    * Returns handler for responsible for process coordination operations.
    */
    public IAeProcessCoordination getProcessCoordination() {
        if (mProcessCoordination == null) {
            mProcessCoordination = new AeProcessCoordinationStub();
        }
        return mProcessCoordination;
    }

    /**
    * Overrides method to remove the process from the ProcessManager.
    *
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineCallback#processEnded(org.activebpel.rt.bpel.IAeBusinessProcess)
    */
    public void processEnded(IAeBusinessProcess aProcess) throws AeBusinessProcessException {
        try {
            getProcessManager().processEnded(aProcess.getProcessId());
        } catch (Throwable t) {
            AeBusinessProcessException bpe = new AeBusinessProcessException(t.getMessage(), t);
            throw bpe;
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#getStartDate()
    */
    public Date getStartDate() {
        return mStarted;
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#getProcessState(long)
    */
    public Document getProcessState(long aProcessId) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessByIdNoUpdate(aProcessId);
        try {
            return process.serializeState(false);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#getProcessVariable(long, java.lang.String)
    */
    public Document getProcessVariable(long aProcessId, String aLocationPath) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessByIdNoUpdate(aProcessId);
        try {
            return process.serializeVariable(aLocationPath);
        } finally {
            releaseProcess(process);
        }
    }

    public String getLocationPathById(long aProcessId, int aLocationId) throws AeBusinessProcessException {
        IAeBusinessProcess process = getProcessByIdNoUpdate(aProcessId);
        try {
            return process.getLocationPath(aLocationId);
        } finally {
            releaseProcess(process);
        }
    }

    /**
    * Dispatches alarm to a process by the location path of the alarm receiver.
    *
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#dispatchAlarm(long, int, int, int)
    */
    public void dispatchAlarm(long aProcessId, int aLocationPathId, int aGroupId, int aAlarmId) throws AeBusinessProcessException {
        long journalId = getQueueManager().removeAlarmForDispatch(aProcessId, aGroupId, aLocationPathId, aAlarmId);
        if (journalId != IAeProcessManager.NULL_JOURNAL_ID) {
            IAeBusinessProcess process = getProcessById(aProcessId);
            try {
                process.dispatchAlarm(aLocationPathId, aAlarmId);
                journalEntryDone(process, journalId);
            } finally {
                releaseProcess(process);
            }
        }
    }

    /**
    * Returns the process instance details for the process with the specified process id.
    *
    * @param aProcessId
    * @return AeProcessInstanceDetail
    */
    public AeProcessInstanceDetail getProcessInstanceDetails(long aProcessId) {
        return getProcessManager().getProcessInstanceDetails(aProcessId);
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#addEngineListener(org.activebpel.rt.bpel.IAeEngineListener)
    */
    public void addEngineListener(IAeEngineListener aListener) {
        mEngineListeners.add(aListener);
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#removeEngineListener(org.activebpel.rt.bpel.IAeEngineListener)
    */
    public void removeEngineListener(IAeEngineListener aListener) {
        mEngineListeners.remove(aListener);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#fireEngineEvent(org.activebpel.rt.bpel.IAeEngineEvent)
    */
    public void fireEngineEvent(IAeEngineEvent aEvent) {
        boolean suspend = false;
        for (Iterator iter = mEngineListeners.iterator(); iter.hasNext(); ) {
            try {
                suspend |= ((IAeEngineListener) iter.next()).handleEngineEvent(aEvent);
            } catch (Throwable th) {
                AeException.logError(th);
            }
        }
        if (suspend && aEvent.getPID() >= 0) {
            try {
                suspendProcess(aEvent.getPID());
            } catch (AeBusinessProcessException ex) {
                ex.logError();
            }
        }
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#fireEngineAlert(org.activebpel.rt.bpel.IAeEngineAlert)
    */
    public void fireEngineAlert(IAeEngineAlert aAlert) {
        for (Iterator iter = mEngineListeners.iterator(); iter.hasNext(); ) {
            try {
                ((IAeEngineListener) iter.next()).handleAlert(aAlert);
            } catch (Throwable th) {
                AeException.logError(th);
            }
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#addMonitorListener(org.activebpel.rt.bpel.IAeMonitorListener)
    */
    public void addMonitorListener(IAeMonitorListener aListener) {
        mMonitorListeners.add(aListener);
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#removeMonitorListener(org.activebpel.rt.bpel.IAeMonitorListener)
    */
    public void removeMonitorListener(IAeMonitorListener aListener) {
        mMonitorListeners.remove(aListener);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#fireMonitorEvent(int, long)
    */
    public void fireMonitorEvent(int aMonitorId, long aEventData) {
        for (Iterator iter = mMonitorListeners.iterator(); iter.hasNext(); ) {
            try {
                ((IAeMonitorListener) iter.next()).handleMonitorEvent(aMonitorId, aEventData);
            } catch (Throwable th) {
                AeException.logError(th);
            }
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#addProcessListener(org.activebpel.rt.bpel.IAeProcessListener)
    */
    public void addProcessListener(IAeProcessListener aListener) {
        mGlobalProcessListeners.add(aListener);
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#removeProcessListener(org.activebpel.rt.bpel.IAeProcessListener)
    */
    public void removeProcessListener(IAeProcessListener aListener) {
        mGlobalProcessListeners.remove(aListener);
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#addProcessListener(org.activebpel.rt.bpel.IAeProcessListener, long)
    */
    public void addProcessListener(IAeProcessListener aListener, long aPid) {
        String key = Long.toString(aPid);
        synchronized (mProcessListeners) {
            Collection listeners = (Collection) mProcessListeners.get(key);
            if (listeners == null) mProcessListeners.put(key, (listeners = new AeSafelyViewableCollection(new LinkedHashSet())));
            listeners.add(aListener);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#removeProcessListener(org.activebpel.rt.bpel.IAeProcessListener, long)
    */
    public void removeProcessListener(IAeProcessListener aListener, long aPid) {
        String key = Long.toString(aPid);
        synchronized (mProcessListeners) {
            Collection listeners = (Collection) mProcessListeners.get(key);
            if (listeners != null) {
                listeners.remove(aListener);
                if (listeners.size() == 0) mProcessListeners.put(key, null);
            }
        }
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#fireInfoEvent(org.activebpel.rt.bpel.IAeProcessInfoEvent)
    */
    public void fireInfoEvent(IAeProcessInfoEvent aInfoEvent) {
        Collection listeners;
        String key = Long.toString(aInfoEvent.getPID());
        synchronized (mProcessListeners) {
            listeners = (Collection) mProcessListeners.get(key);
        }
        if (listeners != null) {
            for (Iterator iter = listeners.iterator(); iter.hasNext(); ) {
                try {
                    ((IAeProcessListener) iter.next()).handleProcessInfoEvent(aInfoEvent);
                } catch (Throwable th) {
                    AeException.logError(th);
                }
            }
        }
        for (Iterator iter = mGlobalProcessListeners.iterator(); iter.hasNext(); ) {
            try {
                ((IAeProcessListener) iter.next()).handleProcessInfoEvent(aInfoEvent);
            } catch (Throwable th) {
                AeException.logError(th);
            }
        }
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#fireEvaluationEvent(long, java.lang.String, int, java.lang.String, java.lang.String)
    */
    public void fireEvaluationEvent(long aPID, String aExpression, int aEventID, String aNodePath, String aResult) {
        fireInfoEvent(new AeProcessInfoEvent(aPID, aNodePath, aEventID, "", aResult));
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#fireEvent(org.activebpel.rt.bpel.IAeProcessEvent)
    */
    public void fireEvent(IAeProcessEvent aEvent) {
        Collection listeners;
        String key = Long.toString(aEvent.getPID());
        boolean suspend = false;
        synchronized (mProcessListeners) {
            listeners = (Collection) mProcessListeners.get(key);
        }
        if (listeners != null) {
            for (Iterator iter = listeners.iterator(); iter.hasNext(); ) {
                try {
                    suspend |= ((IAeProcessListener) iter.next()).handleProcessEvent(aEvent);
                } catch (Throwable th) {
                    AeException.logError(th);
                }
            }
        }
        for (Iterator iter = mGlobalProcessListeners.iterator(); iter.hasNext(); ) {
            try {
                suspend |= ((IAeProcessListener) iter.next()).handleProcessEvent(aEvent);
            } catch (Throwable th) {
                AeException.logError(th);
            }
        }
        if (suspend) {
            try {
                suspendProcess(aEvent.getPID());
            } catch (AeBusinessProcessException e) {
                e.logError();
            }
        }
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessFactory#createProcess(long, org.activebpel.rt.bpel.impl.IAeProcessPlan)
    */
    public IAeBusinessProcess createProcess(long aPid, IAeProcessPlan aPlan) {
        AeBusinessProcess process = new AeBusinessProcess(aPid, this, aPlan);
        return process;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#start()
    */
    public void start() throws AeBusinessProcessException {
        mStarted = new Date();
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#stop()
    */
    public void stop() throws AeBusinessProcessException {
        mStarted = null;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#shutDown()
    */
    public void shutDown() throws AeBusinessProcessException {
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getTypeMapping()
    */
    public AeTypeMapping getTypeMapping() {
        return mTypeMapping;
    }

    /**
    * Returns the strategy for managing partner links.
    */
    public IAeEnginePartnerLinkStrategy getPartnerLinkStrategy() {
        if (mPartnerLinkStrategy == null) {
            mPartnerLinkStrategy = new AeDefaultPartnerLinkStrategy();
        }
        return mPartnerLinkStrategy;
    }

    /**
    * Sets the strategy for managing partner link.
    */
    protected void setPartnerLinkStrategy(IAeEnginePartnerLinkStrategy aPartnerLinkStrategy) {
        mPartnerLinkStrategy = aPartnerLinkStrategy;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#scheduleAlarm(long, int, int, int, java.util.Date)
    */
    public void scheduleAlarm(long aProcessId, int aLocationPathId, int aGroupId, int aAlarmId, Date aDeadline) throws AeBusinessProcessException {
        getQueueManager().scheduleAlarm(aProcessId, aLocationPathId, aGroupId, aAlarmId, aDeadline);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#removeAlarm(long, int, int)
    */
    public boolean removeAlarm(long aProcessId, int aLocationPathId, int aAlarmId) throws AeBusinessProcessException {
        return getQueueManager().removeAlarm(aProcessId, aLocationPathId, aAlarmId);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#addInvoke(org.activebpel.rt.bpel.impl.IAeProcessPlan, org.activebpel.rt.bpel.impl.IAeInvokeInternal)
    */
    public void addInvoke(IAeProcessPlan aPlan, IAeInvokeInternal aInvokeQueueObject) throws AeBusinessProcessException {
        getQueueManager().addInvoke(aPlan, aInvokeQueueObject);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#addMessageReceiver(org.activebpel.rt.bpel.impl.queue.AeMessageReceiver)
    */
    public void addMessageReceiver(AeMessageReceiver aMessageReceiver) throws AeBusinessProcessException {
        getQueueManager().addMessageReceiver(aMessageReceiver);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#removeMessageReceiver(long, int)
    */
    public boolean removeMessageReceiver(long aProcessId, int aMessageReceiverPathId) throws AeBusinessProcessException {
        return getQueueManager().removeMessageReceiver(aProcessId, aMessageReceiverPathId);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#sendReply(org.activebpel.rt.bpel.impl.queue.AeReply, org.activebpel.rt.message.IAeMessageData, org.activebpel.rt.bpel.IAeFault, java.util.Map)
    */
    public void sendReply(AeReply aReplyObject, IAeMessageData aData, IAeFault aFault, Map aProcessProperties) throws AeBusinessProcessException {
        getQueueManager().sendReply(aReplyObject, aData, aFault, aProcessProperties);
        getProcessManager().journalSentReply(aReplyObject.getProcessId(), aReplyObject, aProcessProperties);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#removeReply(org.activebpel.rt.bpel.impl.queue.AeReply)
    */
    public void removeReply(AeReply aReplyObject) {
        getQueueManager().removeReply(aReplyObject);
    }

    /**
    * Implements a default "do nothing" strategy for managing partner links.
    */
    protected static class AeDefaultPartnerLinkStrategy implements IAeEnginePartnerLinkStrategy {

        /**
       * @see org.activebpel.rt.bpel.impl.IAeEnginePartnerLinkStrategy#initPartnerLink(org.activebpel.rt.bpel.impl.AePartnerLink, org.activebpel.rt.bpel.impl.IAeProcessPlan)
       */
        public void initPartnerLink(AePartnerLink aPartnerLink, IAeProcessPlan aPlan) throws AeBusinessProcessException {
        }

        /**
       * @see org.activebpel.rt.bpel.impl.IAeEnginePartnerLinkStrategy#updatePartnerLink(org.activebpel.rt.bpel.impl.AePartnerLink, org.activebpel.rt.bpel.impl.IAeProcessPlan, org.activebpel.wsio.receive.IAeMessageContext)
       */
        public void updatePartnerLink(AePartnerLink aPartnerLink, IAeProcessPlan aPlan, IAeMessageContext aMessageContext) throws AeBusinessProcessException {
        }
    }

    /**
    * TODO (cck) need a type uri to object type mapping factory.
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#loadResource(java.lang.String, java.lang.String)
    */
    public final Object loadResource(String aLocation, String aTypeURI) throws AeException {
        return loadResourceInternal(getURNResolver().getURL(aLocation), aTypeURI);
    }

    /**
    * Loadresource for resolved location.
    * @param aResolvedLocation
    * @param aTypeURI
    * @throws AeException
    */
    protected Object loadResourceInternal(String aResolvedLocation, String aTypeURI) throws AeException {
        try {
            URL url = new URL(aResolvedLocation);
            InputSource source = new InputSource(url.openStream());
            source.setSystemId(url.toExternalForm());
            return source;
        } catch (Throwable ex) {
            throw new AeException(ex);
        }
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#isRunning()
    */
    public boolean isRunning() {
        return getStartDate() != null;
    }

    /**
    * Adds a custom manager to the engine's manager list.
    *
    * @param aManagerName
    * @param aManager
    */
    public void addCustomManager(String aManagerName, IAeManager aManager) throws AeException {
        if (getCustomManagers().containsKey(aManagerName)) throw new AeException(AeMessages.format("AeBusinessProcessEngine.DuplicateCustomManagerError", aManagerName));
        getCustomManagers().put(aManagerName, aManager);
        aManager.setEngine(this);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getCustomManager(java.lang.String)
    */
    public IAeManager getCustomManager(String aManagerName) {
        return (IAeManager) getCustomManagers().get(aManagerName);
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getCustomManagerNames()
    */
    public Iterator getCustomManagerNames() {
        return new HashSet(getCustomManagers().keySet()).iterator();
    }

    /**
    * @return Returns the customManagerMap.
    */
    protected Map getCustomManagers() {
        return mCustomManagers;
    }

    /**
    * @param aCustomManagerMap the customManagerMap to set
    */
    protected void setCustomManagers(Map aCustomManagerMap) {
        mCustomManagers = aCustomManagerMap;
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#getURNResolver()
    */
    public IAeURNResolver getURNResolver() {
        if (mURNResolver == null) {
            mURNResolver = new AeURNResolver();
        }
        return mURNResolver;
    }

    /**
    * Setter for the urn resolver
    * @param aResolver
    */
    public void setURNResolver(IAeURNResolver aResolver) {
        mURNResolver = aResolver;
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#getMonitorStatus()
    */
    public int getMonitorStatus() {
        return mMonitorStatus;
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#setMonitorStatus(int)
    */
    public void setMonitorStatus(int aMonitorStatus) {
        mMonitorStatus = aMonitorStatus;
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#isRestartable(long)
    */
    public boolean isRestartable(long aProcessId) throws AeBusinessProcessException {
        return false;
    }

    /**
    * @see org.activebpel.rt.bpel.IAeBusinessProcessEngine#restartProcess(long)
    */
    public void restartProcess(long aProcessId) throws AeBusinessProcessException {
        throw new UnsupportedOperationException(AeMessages.format("AeBusinessProcessEngine.ERROR_RestartProcessNotSupported", String.valueOf(aProcessId)));
    }

    /**
    * @see org.activebpel.rt.bpel.impl.IAeBusinessProcessEngineInternal#isSuspendSupported()
    */
    public boolean isSuspendSupported() {
        return true;
    }
}
