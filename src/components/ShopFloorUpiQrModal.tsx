import React, { useState, useEffect, useId } from 'react';
import QRCode from 'qrcode';
import {
  QrCode,
  X,
  Copy,
  Check,
  Download,
  Printer,
  Volume2,
  Sparkles,
  IndianRupee,
  ShieldCheck,
  Store,
  FileText,
  Smartphone,
  Share2,
  RefreshCw
} from 'lucide-react';

interface ShopFloorUpiQrModalProps {
  isOpen: boolean;
  onClose: () => void;
  defaultAmount?: number;
  defaultOrderId?: string;
}

export const ShopFloorUpiQrModal: React.FC<ShopFloorUpiQrModalProps> = ({
  isOpen,
  onClose,
  defaultAmount = 150,
  defaultOrderId = 'QD-STORE-892',
}) => {
  const [qrType, setQrType] = useState<'DYNAMIC' | 'STATIC'>('DYNAMIC');
  const [merchantVpa, setMerchantVpa] = useState<string>('quickdelivery.pay@icici');
  const [merchantName, setMerchantName] = useState<string>('Quick Delivery Logistics Hub');
  const [amountInr, setAmountInr] = useState<string>(defaultAmount.toString());
  const [orderRef, setOrderRef] = useState<string>(defaultOrderId);
  const [transactionNote, setTransactionNote] = useState<string>('Shop Floor Parcel Dispatch');
  
  const [qrDataUrl, setQrDataUrl] = useState<string>('');
  const [qrError, setQrError] = useState<string | null>(null);
  const [copiedLink, setCopiedLink] = useState<boolean>(false);
  const [soundboxPlaying, setSoundboxPlaying] = useState<boolean>(false);
  const [soundboxNotice, setSoundboxNotice] = useState<string | null>(null);

  // Generate UPI URL according to NPCI UPI Specifications
  // upi://pay?pa={vpa}&pn={payeeName}&am={amount}&cu=INR&tn={transactionNote}&tr={refUrl}
  const upiIntentUrl = React.useMemo(() => {
    const params = new URLSearchParams();
    params.set('pa', merchantVpa.trim());
    params.set('pn', merchantName.trim());
    params.set('cu', 'INR');
    
    if (qrType === 'DYNAMIC') {
      const cleanAmt = parseFloat(amountInr);
      if (!isNaN(cleanAmt) && cleanAmt > 0) {
        params.set('am', cleanAmt.toFixed(2));
      }
      if (orderRef.trim()) {
        params.set('tr', orderRef.trim());
      }
      if (transactionNote.trim()) {
        params.set('tn', `${transactionNote.trim()} [${orderRef.trim() || 'POS'}]`);
      }
    } else {
      // Static Standee QR (open amount, customer enters payment amount in their UPI app)
      params.set('tn', 'Quick Delivery Shop Floor Counter');
    }

    return `upi://pay?${params.toString()}`;
  }, [qrType, merchantVpa, merchantName, amountInr, orderRef, transactionNote]);

  // Generate QR Code data URL whenever parameters change
  useEffect(() => {
    let isSubscribed = true;
    QRCode.toDataURL(upiIntentUrl, {
      width: 380,
      margin: 1.5,
      color: {
        dark: '#042F2E', // brand deep emerald
        light: '#FFFFFF',
      },
      errorCorrectionLevel: 'M',
    })
      .then((url) => {
        if (isSubscribed) {
          setQrDataUrl(url);
          setQrError(null);
        }
      })
      .catch((err) => {
        if (isSubscribed) {
          setQrError('Failed to generate QR code');
          console.error('QR generation error:', err);
        }
      });

    return () => {
      isSubscribed = false;
    };
  }, [upiIntentUrl]);

  if (!isOpen) return null;

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(upiIntentUrl);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2500);
    } catch {
      // fallback
      const ta = document.createElement('textarea');
      ta.value = upiIntentUrl;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2500);
    }
  };

  const handleDownloadQr = () => {
    if (!qrDataUrl) return;
    const link = document.createElement('a');
    link.href = qrDataUrl;
    link.download = `QuickDelivery-UPI-QR-${qrType}-${orderRef || 'Counter'}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handlePrintStandee = () => {
    window.print();
  };

  const handleSimulateSoundbox = () => {
    setSoundboxPlaying(true);
    const spokenAmount = qrType === 'DYNAMIC' && parseFloat(amountInr) > 0 ? amountInr : '150';
    const message = `🔔 Payment of ₹${spokenAmount} received successfully on Quick Delivery UPI!`;
    setSoundboxNotice(message);

    // Realistic Soundbox Text-to-Speech synthesis
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(
        `Payment of rupees ${spokenAmount} received on Quick Delivery U P I.`
      );
      utterance.rate = 1.0;
      utterance.pitch = 1.1;
      utterance.onend = () => setSoundboxPlaying(false);
      utterance.onerror = () => setSoundboxPlaying(false);
      window.speechSynthesis.speak(utterance);
    } else {
      setTimeout(() => setSoundboxPlaying(false), 3000);
    }

    setTimeout(() => {
      setSoundboxNotice(null);
    }, 6000);
  };

  const amountPresets = [50, 100, 150, 250, 500, 1000];

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm p-3 sm:p-4 flex items-center justify-center animate-in fade-in overflow-y-auto print:p-0 print:bg-white">
      <div className="w-full max-w-3xl rounded-2xl bg-white border-2 border-emerald-500 shadow-2xl overflow-hidden flex flex-col my-auto max-h-[92vh] print:max-h-none print:border-none print:shadow-none">
        
        {/* Modal Header */}
        <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] text-white px-5 py-4 flex items-center justify-between shrink-0 print:hidden">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/10 border border-emerald-300/40 flex items-center justify-center text-xl shadow-inner">
              <QrCode className="w-6 h-6 text-[#A7F3D0]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-extrabold text-base sm:text-lg leading-tight">
                  Shop-Floor Dynamic &amp; Static UPI QR
                </h3>
                <span className="text-[10px] font-extrabold uppercase tracking-wider bg-emerald-400/20 text-[#A7F3D0] border border-emerald-300/30 px-2 py-0.5 rounded-full">
                  NPCI 0% Fee
                </span>
              </div>
              <p className="text-xs text-emerald-100/85 mt-0.5">
                Generate instant scannable UPI QR for dispatch counter, parcel billing &amp; shop-floor collections
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-emerald-100 hover:text-white transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-4 sm:p-6 overflow-y-auto space-y-5 flex-1 bg-slate-50/50 print:bg-white print:p-2">
          
          {/* Soundbox notification banner */}
          {soundboxNotice && (
            <div className="p-3.5 rounded-xl bg-emerald-900 text-white border-2 border-emerald-400 shadow-lg flex items-center justify-between gap-3 animate-bounce print:hidden">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-full bg-emerald-500 flex items-center justify-center text-white shrink-0 animate-pulse">
                  <Volume2 className="w-4 h-4" />
                </div>
                <div>
                  <div className="text-xs font-extrabold text-[#A7F3D0] uppercase tracking-wider">
                    📢 Shop Soundbox Speaker Alert
                  </div>
                  <div className="text-xs font-bold text-white mt-0.5">
                    {soundboxNotice}
                  </div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSoundboxNotice(null)}
                className="text-emerald-300 hover:text-white text-xs font-extrabold px-2 py-1"
              >
                Dismiss
              </button>
            </div>
          )}

          {/* QR Type Selector: Dynamic vs Static */}
          <div className="grid grid-cols-2 gap-2 p-1.5 rounded-xl bg-slate-200/80 print:hidden">
            <button
              type="button"
              onClick={() => setQrType('DYNAMIC')}
              className={`py-2 px-3 rounded-lg text-xs font-extrabold transition flex items-center justify-center gap-2 cursor-pointer ${
                qrType === 'DYNAMIC'
                  ? 'bg-white text-[#042F2E] shadow-sm border border-emerald-500/30'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Sparkles className="w-3.5 h-3.5 text-[#059669]" />
              <span>Dynamic Bill QR (Locked Amount)</span>
            </button>
            <button
              type="button"
              onClick={() => setQrType('STATIC')}
              className={`py-2 px-3 rounded-lg text-xs font-extrabold transition flex items-center justify-center gap-2 cursor-pointer ${
                qrType === 'STATIC'
                  ? 'bg-white text-[#042F2E] shadow-sm border border-emerald-500/30'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Store className="w-3.5 h-3.5 text-[#059669]" />
              <span>Static Standee QR (Open Amount)</span>
            </button>
          </div>

          {/* Main Grid: Left Controls & Right Standee Preview */}
          <div className="grid grid-cols-1 md:grid-cols-12 gap-5 items-start">
            
            {/* Left Column: Configuration Controls (hidden when printing) */}
            <div className="md:col-span-7 space-y-4 print:hidden">
              
              {/* Dynamic Amount Inputs (only if DYNAMIC) */}
              {qrType === 'DYNAMIC' ? (
                <div className="p-3.5 rounded-xl bg-white border border-emerald-500/30 shadow-xs space-y-3">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-extrabold text-[#042F2E] uppercase tracking-wider flex items-center gap-1.5">
                      <IndianRupee className="w-3.5 h-3.5 text-[#059669]" />
                      <span>Order / Bill Amount (₹)</span>
                    </label>
                    <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                      Auto-locked on scan
                    </span>
                  </div>

                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 font-extrabold text-base">
                      ₹
                    </span>
                    <input
                      type="number"
                      min="1"
                      step="1"
                      value={amountInr}
                      onChange={(e) => setAmountInr(e.target.value)}
                      placeholder="Enter amount in ₹ (e.g. 150)"
                      className="w-full pl-8 pr-4 py-2 text-base font-extrabold bg-slate-50 border border-slate-300 rounded-xl focus:border-[#059669] focus:bg-white text-[#042F2E] outline-none"
                    />
                  </div>

                  {/* Preset chips */}
                  <div className="flex flex-wrap gap-1.5">
                    {amountPresets.map((amt) => (
                      <button
                        key={amt}
                        type="button"
                        onClick={() => setAmountInr(amt.toString())}
                        className={`px-2.5 py-1 rounded-lg text-xs font-extrabold border transition cursor-pointer ${
                          amountInr === amt.toString()
                            ? 'bg-[#042F2E] text-[#A7F3D0] border-[#059669]'
                            : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                        }`}
                      >
                        ₹{amt}
                      </button>
                    ))}
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 border-t border-slate-100">
                    <div>
                      <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                        Order / Invoice Ref
                      </label>
                      <input
                        type="text"
                        value={orderRef}
                        onChange={(e) => setOrderRef(e.target.value)}
                        placeholder="e.g. QD-ORD-892"
                        className="w-full px-2.5 py-1.5 text-xs font-mono font-bold bg-slate-50 border border-slate-200 rounded-lg text-[#042F2E]"
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                        Transaction Note
                      </label>
                      <input
                        type="text"
                        value={transactionNote}
                        onChange={(e) => setTransactionNote(e.target.value)}
                        placeholder="e.g. Parcel Dispatch"
                        className="w-full px-2.5 py-1.5 text-xs font-bold bg-slate-50 border border-slate-200 rounded-lg text-[#042F2E]"
                      />
                    </div>
                  </div>
                </div>
              ) : (
                <div className="p-3.5 rounded-xl bg-white border border-emerald-500/30 shadow-xs space-y-2">
                  <div className="flex items-center gap-2">
                    <Store className="w-4 h-4 text-[#059669]" />
                    <span className="text-xs font-extrabold text-[#042F2E]">
                      Static Counter Standee Mode
                    </span>
                  </div>
                  <p className="text-xs text-slate-600 leading-relaxed">
                    This QR code has <strong>no fixed amount</strong>. Perfect for printing and laminating as a permanent counter standee at the dispatch desk. Walk-in customers or riders scan the standee and type the exact bill amount in Google Pay, PhonePe, Paytm, or BHIM.
                  </p>
                </div>
              )}

              {/* Merchant VPA & Name Details */}
              <div className="p-3.5 rounded-xl bg-white border border-slate-200 shadow-xs space-y-3">
                <div className="text-xs font-extrabold text-slate-700 uppercase tracking-wider flex items-center justify-between">
                  <span>Merchant UPI Account Details</span>
                  <span className="text-[10px] text-[#059669] font-bold">Instant Settlement</span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                      Merchant VPA (UPI ID)
                    </label>
                    <input
                      type="text"
                      value={merchantVpa}
                      onChange={(e) => setMerchantVpa(e.target.value)}
                      placeholder="e.g. quickdelivery.pay@icici"
                      className="w-full px-2.5 py-1.5 text-xs font-mono font-bold bg-slate-50 border border-slate-200 rounded-lg text-[#042F2E]"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                      Payee Business Name
                    </label>
                    <input
                      type="text"
                      value={merchantName}
                      onChange={(e) => setMerchantName(e.target.value)}
                      placeholder="e.g. Quick Delivery Hub"
                      className="w-full px-2.5 py-1.5 text-xs font-bold bg-slate-50 border border-slate-200 rounded-lg text-[#042F2E]"
                    />
                  </div>
                </div>
              </div>

              {/* UPI Intent Deep Link Preview */}
              <div className="p-3 rounded-xl bg-slate-100 border border-slate-200 space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold uppercase text-slate-500">
                    NPCI UPI Deep Link String
                  </span>
                  <button
                    type="button"
                    onClick={handleCopyLink}
                    className="text-[10px] text-[#059669] hover:text-emerald-700 font-extrabold flex items-center gap-1 cursor-pointer"
                  >
                    {copiedLink ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                    <span>{copiedLink ? 'Copied Link!' : 'Copy Intent'}</span>
                  </button>
                </div>
                <div className="font-mono text-[10px] text-slate-600 break-all bg-white p-2 rounded-lg border border-slate-200 select-all">
                  {upiIntentUrl}
                </div>
              </div>

              {/* Action Toolbar */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 pt-1">
                <button
                  type="button"
                  onClick={handleCopyLink}
                  className="py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-extrabold transition flex items-center justify-center gap-1.5 cursor-pointer border border-slate-300"
                >
                  {copiedLink ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-600" />
                      <span>Copied!</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5 text-slate-600" />
                      <span>Copy UPI Link</span>
                    </>
                  )}
                </button>

                <button
                  type="button"
                  onClick={handleDownloadQr}
                  className="py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-extrabold transition flex items-center justify-center gap-1.5 cursor-pointer border border-slate-300"
                >
                  <Download className="w-3.5 h-3.5 text-slate-600" />
                  <span>Save QR PNG</span>
                </button>

                <button
                  type="button"
                  onClick={handlePrintStandee}
                  className="py-2.5 px-3 rounded-xl bg-emerald-100 hover:bg-emerald-200 text-[#042F2E] text-xs font-extrabold transition flex items-center justify-center gap-1.5 cursor-pointer border border-emerald-300"
                >
                  <Printer className="w-3.5 h-3.5 text-[#059669]" />
                  <span>Print Standee</span>
                </button>
              </div>

              {/* Shop Soundbox Simulator Trigger */}
              <button
                type="button"
                onClick={handleSimulateSoundbox}
                disabled={soundboxPlaying}
                className="w-full py-2.5 px-4 rounded-xl bg-gradient-to-r from-emerald-700 via-teal-800 to-emerald-900 hover:from-emerald-800 hover:to-emerald-950 text-white text-xs font-extrabold shadow-sm transition flex items-center justify-center gap-2 cursor-pointer border border-emerald-400/30"
              >
                <Volume2 className={`w-4 h-4 text-[#A7F3D0] ${soundboxPlaying ? 'animate-spin' : ''}`} />
                <span>
                  {soundboxPlaying
                    ? 'Playing Soundbox Voice...'
                    : `Simulate Shop Soundbox Confirmation (${qrType === 'DYNAMIC' ? `₹${amountInr || '0'}` : '₹150'})`}
                </span>
              </button>
            </div>

            {/* Right Column: Print-Ready Shop Counter Standee Card */}
            <div className="md:col-span-5 flex flex-col items-center print:w-full print:max-w-md print:mx-auto">
              
              <div className="w-full max-w-[340px] rounded-2xl bg-white border-2 border-slate-900 shadow-xl overflow-hidden flex flex-col items-center text-center p-4 space-y-3 relative print:border-2 print:border-black print:shadow-none">
                
                {/* Standee Top Branding Banner */}
                <div className="w-full bg-[#042F2E] text-white py-2.5 px-3 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-2 text-left">
                    <div className="w-7 h-7 rounded-lg bg-emerald-500/20 border border-emerald-400/40 flex items-center justify-center text-sm font-black text-[#A7F3D0]">
                      ⚡
                    </div>
                    <div>
                      <div className="font-black text-xs tracking-tight text-white leading-none">
                        QUICK DELIVERY
                      </div>
                      <div className="text-[9px] text-[#A7F3D0] font-bold mt-0.5">
                        Shop-Floor Payment Desk
                      </div>
                    </div>
                  </div>
                  <span className="text-[9px] font-black uppercase px-2 py-0.5 rounded-full bg-emerald-400 text-[#042F2E]">
                    UPI Verified
                  </span>
                </div>

                {/* Amount Pill for Dynamic vs Open for Static */}
                {qrType === 'DYNAMIC' ? (
                  <div className="w-full py-1.5 px-3 rounded-xl bg-emerald-50 border border-emerald-300 flex items-center justify-between">
                    <span className="text-[11px] font-extrabold text-[#042F2E] uppercase">
                      Exact Bill Amount
                    </span>
                    <span className="text-base font-black text-[#059669]">
                      ₹{amountInr || '0'}
                    </span>
                  </div>
                ) : (
                  <div className="w-full py-1.5 px-3 rounded-xl bg-slate-100 border border-slate-200">
                    <span className="text-xs font-black text-slate-800">
                      Scan &amp; Enter Any Bill Amount
                    </span>
                  </div>
                )}

                {/* The QR Code Container */}
                <div className="p-3 bg-white rounded-xl border border-slate-200 shadow-inner flex flex-col items-center justify-center">
                  {qrDataUrl ? (
                    <img
                      src={qrDataUrl}
                      alt="UPI QR Code"
                      className="w-56 h-56 object-contain rounded-lg"
                    />
                  ) : (
                    <div className="w-56 h-56 flex items-center justify-center text-xs text-slate-400">
                      Generating QR...
                    </div>
                  )}
                  {qrError && (
                    <div className="text-[10px] text-red-500 font-bold mt-1">
                      {qrError}
                    </div>
                  )}
                </div>

                {/* Payee Info */}
                <div className="w-full space-y-0.5">
                  <div className="text-xs font-black text-slate-900 truncate">
                    {merchantName}
                  </div>
                  <div className="text-[10px] font-mono font-bold text-slate-500 truncate">
                    UPI ID: {merchantVpa}
                  </div>
                  {qrType === 'DYNAMIC' && orderRef && (
                    <div className="text-[9px] font-mono font-bold text-emerald-700 bg-emerald-50/80 px-2 py-0.5 rounded border border-emerald-200/60 inline-block mt-0.5">
                      REF: {orderRef}
                    </div>
                  )}
                </div>

                {/* Supported Apps Logos Pill */}
                <div className="w-full pt-2 border-t border-slate-200 space-y-1">
                  <div className="text-[9px] font-extrabold uppercase tracking-wider text-slate-400">
                    Accepted on all UPI Apps
                  </div>
                  <div className="flex items-center justify-center gap-1.5 flex-wrap">
                    <span className="px-2 py-0.5 rounded bg-blue-50 text-blue-700 font-extrabold text-[9px] border border-blue-200">
                      Google Pay
                    </span>
                    <span className="px-2 py-0.5 rounded bg-purple-50 text-purple-700 font-extrabold text-[9px] border border-purple-200">
                      PhonePe
                    </span>
                    <span className="px-2 py-0.5 rounded bg-sky-50 text-sky-700 font-extrabold text-[9px] border border-sky-200">
                      Paytm
                    </span>
                    <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 font-extrabold text-[9px] border border-emerald-200">
                      BHIM UPI
                    </span>
                    <span className="px-2 py-0.5 rounded bg-amber-50 text-amber-700 font-extrabold text-[9px] border border-amber-200">
                      CRED
                    </span>
                  </div>
                </div>

                {/* Standee Base Footer */}
                <div className="text-[8px] font-bold text-slate-400 uppercase tracking-widest pt-1">
                  • POWERED BY QUICK DELIVERY UNIFIED HUB •
                </div>

              </div>

              <p className="text-[10px] text-slate-500 text-center mt-2 print:hidden">
                Display this standee on your store / hub counter or print on an acrylic stand.
              </p>

            </div>

          </div>

        </div>

        {/* Modal Footer */}
        <div className="bg-slate-100 border-t border-slate-200 px-5 py-3 flex items-center justify-between shrink-0 print:hidden">
          <div className="flex items-center gap-2 text-xs text-slate-600">
            <ShieldCheck className="w-4 h-4 text-[#059669]" />
            <span className="font-bold">
              NPCI Compliant • Zero Merchant Deduction (0% MDR)
            </span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-900 hover:bg-black text-white text-xs font-extrabold cursor-pointer"
          >
            Done &amp; Close
          </button>
        </div>

      </div>
    </div>
  );
};
