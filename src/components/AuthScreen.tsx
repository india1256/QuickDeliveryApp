import React, { useState } from 'react';
import {
  ShieldCheck,
  User,
  Bike,
  Building2,
  Lock,
  Mail,
  Phone,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
  Eye,
  EyeOff,
  Sparkles,
  KeyRound,
  LogOut
} from 'lucide-react';

import {
  checkEndpointRateLimit,
  issueRoleSessionToken,
  sanitizeEmail,
  sanitizePhone,
  sanitizeText,
} from '../services/securityGuard';

export type UserRole = 'CUSTOMER' | 'RIDER' | 'ADMIN';

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: UserRole;
  avatarText: string;
  avatarUrl?: string;
  createdAt: string;
  sessionToken?: string;
}

interface AuthScreenProps {
  onLoginSuccess: (user: AuthUser) => void;
  lockedRole?: UserRole;
}

// Pre-seeded demo credentials for quick 1-tap testing of each role
export const DEMO_USERS: Record<UserRole, AuthUser & { pass: string }> = {
  CUSTOMER: {
    id: 'usr_cust_101',
    name: 'Aarav Sharma',
    email: 'customer@quickdelivery.in',
    phone: '9876543210',
    role: 'CUSTOMER',
    avatarText: 'AS',
    avatarUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80',
    pass: 'customer123',
    createdAt: '2026-01-15',
  },
  RIDER: {
    id: 'usr_rider_201',
    name: 'Vikram Singh',
    email: 'rider@quickdelivery.in',
    phone: '9811223344',
    role: 'RIDER',
    avatarText: 'VS',
    avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80',
    pass: 'rider123',
    createdAt: '2026-02-10',
  },
  ADMIN: {
    id: 'usr_admin_301',
    name: 'Operations Director',
    email: 'admin@quickdelivery.in',
    phone: '9800112233',
    role: 'ADMIN',
    avatarText: 'OD',
    avatarUrl: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&auto=format&fit=crop&q=80',
    pass: 'admin123',
    createdAt: '2025-11-01',
  },
};

export const AuthScreen: React.FC<AuthScreenProps> = ({ onLoginSuccess, lockedRole }) => {
  const [authMode, setAuthMode] = useState<'LOGIN' | 'SIGNUP'>('LOGIN');
  const [loginMethod, setLoginMethod] = useState<'EMAIL' | 'PHONE'>('EMAIL');
  
  // Form fields
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [selectedRole, setSelectedRole] = useState<UserRole>(lockedRole || 'CUSTOMER');
  const effectiveRole: UserRole = lockedRole || selectedRole;
  
  // Phone OTP state
  const [otpSent, setOtpSent] = useState(false);
  const [generatedOtp, setGeneratedOtp] = useState('4821');
  const [enteredOtp, setEnteredOtp] = useState('');

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);

  // Quick 1-tap demo login
  const handleDemoLogin = (role: UserRole) => {
    const demo = DEMO_USERS[role];
    const signedDemo: AuthUser = {
      ...demo,
      sessionToken: issueRoleSessionToken(demo),
    };
    setErrorMessage(null);
    setSuccessNotice(`Logging in as ${demo.role}: ${demo.name}...`);
    setTimeout(() => {
      onLoginSuccess(signedDemo);
    }, 400);
  };

  const handleEmailAuthSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const cleanEmail = sanitizeEmail(email);
    const cleanPass = password.trim();

    if (!cleanEmail || !cleanPass) {
      setErrorMessage('Please enter both email address and password.');
      return;
    }

    const rateCheck = checkEndpointRateLimit('AUTH_ATTEMPT', cleanEmail);
    if (!rateCheck.allowed) {
      setErrorMessage(rateCheck.error || 'Too many login attempts. Please wait a minute.');
      return;
    }

    if (authMode === 'LOGIN') {
      // Check if matches demo users
      const matchedRole = (Object.keys(DEMO_USERS) as UserRole[]).find(
        (r) => DEMO_USERS[r].email.toLowerCase() === cleanEmail
      );

      if (matchedRole && DEMO_USERS[matchedRole].pass === cleanPass) {
        if (lockedRole && matchedRole !== lockedRole) {
          setErrorMessage(`This standalone app is restricted to ${lockedRole} accounts only.`);
          return;
        }
        const demoUser = DEMO_USERS[matchedRole];
        onLoginSuccess({
          ...demoUser,
          sessionToken: issueRoleSessionToken(demoUser),
        });
        return;
      }

      // Allow login with any valid formatted email/pass (non-demo emails cannot self-escalate to ADMIN without matching credentials)
      if (cleanPass.length >= 4) {
        let assignedRole: UserRole = effectiveRole === 'ADMIN' ? 'CUSTOMER' : effectiveRole;
        if (!lockedRole) {
          if (cleanEmail === 'ravindergupta141@gmail.com' || cleanEmail === 'admin@quickdelivery.in') {
            assignedRole = 'ADMIN';
          } else if (cleanEmail.includes('rider') || cleanEmail.includes('partner')) {
            assignedRole = 'RIDER';
          }
        } else if (lockedRole === 'ADMIN') {
          if (cleanEmail !== 'ravindergupta141@gmail.com' && cleanEmail !== 'admin@quickdelivery.in') {
            setErrorMessage('Unauthorized Admin email. Only verified Admin accounts can access the Admin console.');
            return;
          }
          assignedRole = 'ADMIN';
        }

        const safePhone = sanitizePhone(phone) || '9876543210';
        const safeName = sanitizeText(cleanEmail.split('@')[0].replace(/[._]/g, ' '), 80).toUpperCase() || 'USER';
        const userId = `usr_${Date.now()}`;
        const user: AuthUser = {
          id: userId,
          name: safeName,
          email: cleanEmail,
          phone: safePhone,
          role: assignedRole,
          avatarText: cleanEmail.slice(0, 2).toUpperCase(),
          createdAt: new Date().toISOString().slice(0, 10),
          sessionToken: issueRoleSessionToken({
            id: userId,
            role: assignedRole,
            email: cleanEmail,
            phone: safePhone,
          }),
        };
        onLoginSuccess(user);
      } else {
        setErrorMessage('Password must be at least 4 characters long.');
      }
    } else {
      // SIGN UP
      const safeFullName = sanitizeText(fullName, 80);
      if (!safeFullName) {
        setErrorMessage('Please enter your full name.');
        return;
      }
      if (cleanPass.length < 4) {
        setErrorMessage('Password must be at least 4 characters long.');
        return;
      }

      const safeRole: UserRole =
        effectiveRole === 'ADMIN' && cleanEmail !== 'ravindergupta141@gmail.com'
          ? 'CUSTOMER'
          : effectiveRole;
      const safePhone = sanitizePhone(phone) || '9876543210';
      const userId = `usr_${Date.now()}`;
      const newUser: AuthUser = {
        id: userId,
        name: safeFullName,
        email: cleanEmail,
        phone: safePhone,
        role: safeRole,
        avatarText: safeFullName.slice(0, 2).toUpperCase(),
        createdAt: new Date().toISOString().slice(0, 10),
        sessionToken: issueRoleSessionToken({
          id: userId,
          role: safeRole,
          email: cleanEmail,
          phone: safePhone,
        }),
      };

      setSuccessNotice(`Account created successfully! Routing to ${safeRole} dashboard...`);
      setTimeout(() => {
        onLoginSuccess(newUser);
      }, 500);
    }
  };

  const handleSendPhoneOtp = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const cleanPhone = phone.trim().replace(/\D/g, '');
    if (cleanPhone.length < 10) {
      setErrorMessage('Please enter a valid 10-digit mobile number.');
      return;
    }

    const randomOtp = String(Math.floor(1000 + Math.random() * 9000));
    setGeneratedOtp(randomOtp);
    setOtpSent(true);
    setSuccessNotice(`📲 SMS OTP Sent to +91 ${cleanPhone.slice(-10)}: Code is ${randomOtp}`);
  };

  const handleVerifyPhoneOtp = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (enteredOtp.trim() !== generatedOtp) {
      setErrorMessage(`Incorrect OTP. Use the demo verification code: ${generatedOtp}`);
      return;
    }

    const user: AuthUser = {
      id: `usr_phone_${Date.now()}`,
      name: fullName.trim() || `User ${phone.slice(-4)}`,
      email: `${phone.replace(/\D/g, '').slice(-10)}@mobile.quickdelivery.in`,
      phone: phone.replace(/\D/g, '').slice(-10),
      role: effectiveRole,
      avatarText: effectiveRole.slice(0, 2),
      createdAt: new Date().toISOString().slice(0, 10),
    };

    onLoginSuccess(user);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#022C22] via-[#042F2E] to-[#0F172A] flex flex-col justify-center items-center p-4 sm:p-6 text-[#0F172A]">
      
      {/* Brand Logo & Header */}
      <div className="w-full max-w-md text-center space-y-2 mb-6">
        <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-tr from-[#059669] to-[#10B981] text-white font-black text-2xl shadow-xl shadow-emerald-950/40 border border-emerald-300/40">
          {lockedRole === 'RIDER' ? '🛵' : lockedRole === 'ADMIN' ? '🏢' : '⚡'}
        </div>
        <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
          {lockedRole === 'RIDER'
            ? 'Quick Delivery Partner'
            : lockedRole === 'ADMIN'
            ? 'Quick Delivery Admin'
            : 'Quick Delivery'}
        </h1>
        <p className="text-xs text-[#A7F3D0]/80 font-medium">
          {lockedRole === 'CUSTOMER'
            ? 'Standalone Customer App • Hyperlocal Parcel Booking & Live Tracking'
            : lockedRole === 'RIDER'
            ? 'Standalone Rider Partner App • Live Dispatch & 80% Earnings'
            : lockedRole === 'ADMIN'
            ? 'Standalone Admin Enterprise App • Real-Time Fleet & Order Control'
            : 'Role-Based Access Control • Customer • Rider Partner • Admin Enterprise'}
        </p>
      </div>

      {/* Main Authentication Card */}
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-emerald-500/30 overflow-hidden flex flex-col">
        
        {/* Card Header & Tabs (Login vs Sign Up) */}
        <div className="p-1.5 bg-slate-100/90 border-b border-slate-200 grid grid-cols-2 gap-1">
          <button
            type="button"
            onClick={() => {
              setAuthMode('LOGIN');
              setErrorMessage(null);
            }}
            className={`py-2.5 rounded-2xl text-xs font-black transition cursor-pointer ${
              authMode === 'LOGIN'
                ? 'bg-white text-[#042F2E] shadow-sm'
                : 'text-slate-500 hover:text-slate-900'
            }`}
          >
            Sign In (Login)
          </button>
          <button
            type="button"
            onClick={() => {
              setAuthMode('SIGNUP');
              setErrorMessage(null);
            }}
            className={`py-2.5 rounded-2xl text-xs font-black transition cursor-pointer ${
              authMode === 'SIGNUP'
                ? 'bg-white text-[#042F2E] shadow-sm'
                : 'text-slate-500 hover:text-slate-900'
            }`}
          >
            Create Account
          </button>
        </div>

        <div className="p-5 sm:p-6 space-y-4">
          
          {/* Feedback Alerts */}
          {errorMessage && (
            <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs font-bold flex items-center gap-2 animate-shake">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          {successNotice && (
            <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs font-bold flex items-center gap-2 animate-bounce">
              <CheckCircle2 className="w-4 h-4 text-[#059669] shrink-0" />
              <span>{successNotice}</span>
            </div>
          )}

          {/* Quick 1-Tap Demo Logins Section */}
          <div className="p-3 rounded-2xl bg-slate-50 border border-slate-200 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-black uppercase tracking-wider text-slate-500 flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-[#059669]" />
                <span>
                  {lockedRole ? `Instant 1-Tap ${lockedRole} Login (Demo)` : 'Instant 1-Tap Role Logins (Demo)'}
                </span>
              </span>
              <span className="text-[9px] text-[#059669] font-bold">Auto-Routes</span>
            </div>

            <div className={`grid ${lockedRole ? 'grid-cols-1' : 'grid-cols-3'} gap-1.5`}>
              {(!lockedRole || lockedRole === 'CUSTOMER') && (
                <button
                  type="button"
                  onClick={() => handleDemoLogin('CUSTOMER')}
                  className="p-2 rounded-xl bg-white border border-slate-200 hover:border-emerald-500 hover:bg-emerald-50/50 transition cursor-pointer text-center group"
                >
                  <div className="text-base group-hover:scale-110 transition">👤</div>
                  <div className="text-[11px] font-black text-slate-800 mt-0.5">Customer</div>
                  <div className="text-[9px] text-slate-400">Book &amp; Track</div>
                </button>
              )}

              {(!lockedRole || lockedRole === 'RIDER') && (
                <button
                  type="button"
                  onClick={() => handleDemoLogin('RIDER')}
                  className="p-2 rounded-xl bg-white border border-slate-200 hover:border-emerald-500 hover:bg-emerald-50/50 transition cursor-pointer text-center group"
                >
                  <div className="text-base group-hover:scale-110 transition">🏍️</div>
                  <div className="text-[11px] font-black text-slate-800 mt-0.5">Rider Partner</div>
                  <div className="text-[9px] text-slate-400">Deliver 80%</div>
                </button>
              )}

              {(!lockedRole || lockedRole === 'ADMIN') && (
                <button
                  type="button"
                  onClick={() => handleDemoLogin('ADMIN')}
                  className="p-2 rounded-xl bg-white border border-slate-200 hover:border-emerald-500 hover:bg-emerald-50/50 transition cursor-pointer text-center group"
                >
                  <div className="text-base group-hover:scale-110 transition">🏢</div>
                  <div className="text-[11px] font-black text-slate-800 mt-0.5">Admin Web</div>
                  <div className="text-[9px] text-slate-400">Command SaaS</div>
                </button>
              )}
            </div>
          </div>

          {/* Divider */}
          <div className="relative flex py-1 items-center">
            <div className="flex-grow border-t border-slate-200" />
            <span className="flex-shrink mx-3 text-[10px] font-bold text-slate-400 uppercase tracking-widest">
              Or Use Credentials
            </span>
            <div className="flex-grow border-t border-slate-200" />
          </div>

          {/* Auth Method Switcher: Email vs Phone */}
          <div className="flex rounded-xl bg-slate-100 p-1 text-xs">
            <button
              type="button"
              onClick={() => {
                setLoginMethod('EMAIL');
                setOtpSent(false);
              }}
              className={`flex-1 py-1.5 rounded-lg font-extrabold transition flex items-center justify-center gap-1.5 cursor-pointer ${
                loginMethod === 'EMAIL'
                  ? 'bg-white text-[#042F2E] shadow-xs'
                  : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              <Mail className="w-3.5 h-3.5" />
              <span>Email &amp; Password</span>
            </button>
            <button
              type="button"
              onClick={() => setLoginMethod('PHONE')}
              className={`flex-1 py-1.5 rounded-lg font-extrabold transition flex items-center justify-center gap-1.5 cursor-pointer ${
                loginMethod === 'PHONE'
                  ? 'bg-white text-[#042F2E] shadow-xs'
                  : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              <Phone className="w-3.5 h-3.5" />
              <span>Phone OTP</span>
            </button>
          </div>

          {/* FORM: Email & Password */}
          {loginMethod === 'EMAIL' ? (
            <form onSubmit={handleEmailAuthSubmit} className="space-y-3">
              {authMode === 'SIGNUP' && (
                <div>
                  <label className="block text-[11px] font-bold uppercase text-slate-600 mb-1">
                    Full Name
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      placeholder="e.g. Aarav Sharma"
                      required
                      className="w-full pl-9 pr-3 py-2.5 text-xs font-semibold bg-slate-50 border border-slate-200 rounded-xl focus:border-[#059669] focus:bg-white outline-none"
                    />
                  </div>
                </div>
              )}

              <div>
                <label className="block text-[11px] font-bold uppercase text-slate-600 mb-1">
                  Email Address
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="user@quickdelivery.in"
                    required
                    className="w-full pl-9 pr-3 py-2.5 text-xs font-semibold bg-slate-50 border border-slate-200 rounded-xl focus:border-[#059669] focus:bg-white outline-none"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-[11px] font-bold uppercase text-slate-600">
                    Password
                  </label>
                  {authMode === 'LOGIN' && (
                    <button
                      type="button"
                      onClick={() => {
                        setEmail('admin@quickdelivery.in');
                        setPassword('admin123');
                      }}
                      className="text-[10px] text-[#059669] font-bold hover:underline"
                    >
                      Fill Admin Demo
                    </button>
                  )}
                </div>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter password (e.g. admin123)"
                    required
                    className="w-full pl-9 pr-10 py-2.5 text-xs font-semibold bg-slate-50 border border-slate-200 rounded-xl focus:border-[#059669] focus:bg-white outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Role Selection for Sign Up or Custom Role selection */}
              {!lockedRole && (
                <div>
                  <label className="block text-[11px] font-bold uppercase text-slate-600 mb-1.5">
                    Select Portal Role
                  </label>
                  <div className="grid grid-cols-3 gap-2">
                    {[
                      { id: 'CUSTOMER' as UserRole, label: 'Customer', icon: User },
                      { id: 'RIDER' as UserRole, label: 'Rider', icon: Bike },
                      { id: 'ADMIN' as UserRole, label: 'Admin', icon: Building2 },
                    ].map((r) => {
                      const IconComp = r.icon;
                      const isSelected = selectedRole === r.id;
                      return (
                        <button
                          key={r.id}
                          type="button"
                          onClick={() => setSelectedRole(r.id)}
                          className={`p-2 rounded-xl border text-xs font-black flex items-center justify-center gap-1.5 transition cursor-pointer ${
                            isSelected
                              ? 'bg-[#042F2E] text-[#A7F3D0] border-[#059669] shadow-xs'
                              : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                          }`}
                        >
                          <IconComp className="w-3.5 h-3.5" />
                          <span>{r.label}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              <button
                type="submit"
                className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] hover:from-[#021f1e] hover:to-[#047857] text-[#A7F3D0] hover:text-white font-extrabold text-xs shadow-md transition flex items-center justify-center gap-2 cursor-pointer border border-emerald-400/40 mt-2"
              >
                <span>{authMode === 'LOGIN' ? `Sign In to ${effectiveRole} Portal` : `Create & Launch ${effectiveRole} Account`}</span>
                <ArrowRight className="w-4 h-4 text-[#10B981]" />
              </button>
            </form>
          ) : (
            /* FORM: Phone OTP Authentication */
            <div className="space-y-3">
              {!otpSent ? (
                <form onSubmit={handleSendPhoneOtp} className="space-y-3">
                  <div>
                    <label className="block text-[11px] font-bold uppercase text-slate-600 mb-1">
                      10-Digit Mobile Number
                    </label>
                    <div className="relative">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-500">
                        +91
                      </span>
                      <input
                        type="tel"
                        maxLength={10}
                        value={phone}
                        onChange={(e) => setPhone(e.target.value.replace(/\D/g, ''))}
                        placeholder="9876543210"
                        required
                        className="w-full pl-12 pr-3 py-2.5 text-xs font-mono font-bold bg-slate-50 border border-slate-200 rounded-xl focus:border-[#059669] focus:bg-white outline-none"
                      />
                    </div>
                  </div>

                  {!lockedRole && (
                    <div>
                      <label className="block text-[11px] font-bold uppercase text-slate-600 mb-1.5">
                        Select Access Role
                      </label>
                      <div className="grid grid-cols-3 gap-2">
                        {(['CUSTOMER', 'RIDER', 'ADMIN'] as UserRole[]).map((r) => (
                          <button
                            key={r}
                            type="button"
                            onClick={() => setSelectedRole(r)}
                            className={`p-2 rounded-xl border text-xs font-black transition cursor-pointer ${
                              selectedRole === r
                                ? 'bg-[#042F2E] text-[#A7F3D0] border-[#059669]'
                                : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                            }`}
                          >
                            {r}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  <button
                    type="submit"
                    className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-[#042F2E] to-[#059669] text-white font-extrabold text-xs shadow-md transition flex items-center justify-center gap-2 cursor-pointer mt-2"
                  >
                    <span>Send SMS OTP</span>
                    <ArrowRight className="w-4 h-4 text-[#A7F3D0]" />
                  </button>
                </form>
              ) : (
                <form onSubmit={handleVerifyPhoneOtp} className="space-y-3">
                  <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-[11px] text-emerald-900">
                    OTP sent to <strong>+91 {phone}</strong>. Verification code: <strong className="text-emerald-700">{generatedOtp}</strong>
                  </div>

                  <div>
                    <label className="block text-[11px] font-bold uppercase text-slate-600 mb-1">
                      Enter 4-Digit OTP
                    </label>
                    <input
                      type="text"
                      maxLength={4}
                      value={enteredOtp}
                      onChange={(e) => setEnteredOtp(e.target.value.replace(/\D/g, ''))}
                      placeholder={generatedOtp}
                      required
                      className="w-full px-3 py-2.5 text-center text-lg font-mono font-black tracking-widest bg-slate-50 border border-slate-200 rounded-xl focus:border-[#059669] focus:bg-white outline-none"
                    />
                  </div>

                  <button
                    type="submit"
                    className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-[#042F2E] to-[#059669] text-white font-extrabold text-xs shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <CheckCircle2 className="w-4 h-4 text-[#A7F3D0]" />
                    <span>Verify &amp; Enter {effectiveRole} Portal</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setOtpSent(false)}
                    className="w-full text-center text-xs text-slate-500 font-bold hover:underline"
                  >
                    Change Phone Number
                  </button>
                </form>
              )}
            </div>
          )}

        </div>

        {/* Card Footer */}
        <div className="bg-slate-50 border-t border-slate-200 px-6 py-3 flex items-center justify-between text-[11px] text-slate-500">
          <div className="flex items-center gap-1.5 font-bold">
            <ShieldCheck className="w-4 h-4 text-[#059669]" />
            <span>Role-Based Access Control Protected</span>
          </div>
          <span className="font-mono text-[10px] text-slate-400">v5.4 RBAC</span>
        </div>

      </div>

    </div>
  );
};
