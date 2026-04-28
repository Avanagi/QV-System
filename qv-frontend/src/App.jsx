import { useState, useEffect } from 'react';
import axios from 'axios';
import {
    Container, Typography, Card, CardContent, Slider, Button,
    List, ListItem, ListItemText, Divider, Chip, TextField, Box,
    CircularProgress, Paper, BottomNavigation, BottomNavigationAction,
    AppBar, Toolbar, IconButton, Grid, Tabs, Tab,
    Switch, FormControlLabel, Checkbox, Stack
} from '@mui/material';
import { motion, AnimatePresence } from 'framer-motion';

import PublicIcon from '@mui/icons-material/Public';
import AssessmentIcon from '@mui/icons-material/Assessment';
import HistoryIcon from '@mui/icons-material/History';
import PersonIcon from '@mui/icons-material/Person';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import GroupIcon from '@mui/icons-material/Group';
import EqualizerIcon from '@mui/icons-material/Equalizer';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import ApiIcon from '@mui/icons-material/Api';
import DashboardIcon from '@mui/icons-material/Dashboard';
import LogoutIcon from '@mui/icons-material/Logout';
import CodeIcon from '@mui/icons-material/Code';

const API_URL = '/api';
const ADMIN_ID = 0;

axios.interceptors.request.use((config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) config.headers['Authorization'] = 'Bearer ' + token;
    return config;
});

const Header = ({ title, onBack }) => (
    <AppBar position="static" color="transparent" elevation={0} sx={{mb: 2}}>
        <Toolbar>
            {onBack && (
                <IconButton edge="start" onClick={onBack} sx={{mr: 1}}>
                    <ArrowBackIcon />
                </IconButton>
            )}
            <Typography variant="h6" sx={{flexGrow: 1, fontWeight: 'bold'}}>
                {title}
            </Typography>
        </Toolbar>
    </AppBar>
);

const PageWrapper = ({ children }) => (
    <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} transition={{ duration: 0.3 }} style={{ paddingBottom: '70px' }}>
        {children}
    </motion.div>
);

function App() {
    const[isAppLoading, setIsAppLoading] = useState(true);
    const [user, setUser] = useState(null);
    const [isAdmin, setIsAdmin] = useState(false);
    const [username, setUsername] = useState('');
    const [appConfig, setAppConfig] = useState({ maxBudget: 100, pollCost: 50 });

    const [navValue, setNavValue] = useState(0);
    const [innerTab, setInnerTab] = useState(0);
    const[authMode, setAuthMode] = useState('login');
    const [activeView, setActiveView] = useState('main');
    const [adminTab, setAdminTab] = useState(0);

    const[publicPolls, setPublicPolls] = useState([]);
    const [myUnlockedPolls, setMyUnlockedPolls] = useState([]);
    const[myCreatedPolls, setMyCreatedPolls] = useState([]);
    const [statsMap, setStatsMap] = useState({});
    const [history, setHistory] = useState([]);
    const[explorerData, setExplorerData] = useState([]);

    const [authLogin, setAuthLogin] = useState('');
    const[authPass, setAuthPass] = useState('');
    const [searchCode, setSearchCode] = useState('');

    const[activePoll, setActivePoll] = useState(null);
    const [votes, setVotes] = useState({});

    const[newTitle, setNewTitle] = useState('');
    const [newDesc, setNewDesc] = useState('');
    const [newOptions, setNewOptions] = useState(['', '']);
    const [isPublic, setIsPublic] = useState(true);
    const [isQuadratic, setIsQuadratic] = useState(true);
    const [createdCode, setCreatedCode] = useState(null);

    const getGroupedHistory = () => {
        const groups = {};
        history.forEach(item => {
            const key = item.pollId || 'unknown';
            if (!groups[key]) {
                groups[key] = {
                    title: item.pollTitle || `Опрос #${key}`,
                    totalCost: 0,
                    items:[]
                };
            }
            groups[key].items.push(item);
            groups[key].totalCost += Number(item.cost || 0);
        });
        return Object.values(groups).reverse();
    };

    useEffect(() => {
        const initApp = async () => {
            if (window.Telegram?.WebApp?.initDataUnsafe?.user) {
                const tgUser = window.Telegram.WebApp.initDataUnsafe.user;
                window.Telegram.WebApp.expand();
                window.Telegram.WebApp.setHeaderColor('#121212');
                await performTgAuth(tgUser.id, tgUser.username || tgUser.first_name);
            } else {
                setIsAppLoading(false);
            }
        };
        initApp();
    },[]);

    useEffect(() => {
        let interval;
        if (activeView === 'main' && navValue === 1 && innerTab === 1 && myCreatedPolls.length > 0) {
            interval = setInterval(() => {
                myCreatedPolls.forEach(async (p) => {
                    try {
                        const statsRes = await axios.get(`${API_URL}/history/stats/${p.id}`);
                        setStatsMap(prev => ({...prev, [p.id]: statsRes.data}));
                    } catch (e) {}
                });
            }, 3000);
        }
        return () => clearInterval(interval);
    },[activeView, navValue, innerTab, myCreatedPolls]);

    const performTgAuth = async (id, name) => {
        try {
            const res = await axios.post(`${API_URL}/auth/login/tg/${id}?username=${name}`);
            await completeLogin(res.data, name);
        } catch (e) { setIsAppLoading(false); }
    };

    const handleWebAuth = async () => {
        if (authLogin === 'admin' && authPass === 'admin') {
            setIsAdmin(true);
            setUser({ userId: ADMIN_ID, balance: '∞' });
            setActiveView('admin');
            return;
        }

        try {
            const endpoint = authMode === 'login' ? '/auth/login/web' : '/auth/register';
            const res = await axios.post(`${API_URL}${endpoint}`, { username: authLogin, password: authPass });
            await completeLogin(res.data, res.data.user.username);
        } catch (e) {
            alert(authMode === 'login' ? "Неверный логин/пароль" : "Пользователь уже существует");
        }
    };

    // ИСПРАВЛЕНИЕ №2: Жестко перезаписываем имя при входе
    const completeLogin = async (data, tgName) => {
        localStorage.setItem('jwt_token', data.token);
        setUser(data.user);

        // Всегда ставим новое имя
        setUsername(data.user.username || tgName || `User ${data.user.userId}`);

        try {
            const conf = await axios.get(`${API_URL}/config`);
            setAppConfig(conf.data);
        } catch (e) {}

        setIsAdmin(false);
        setIsAppLoading(false);
        setActiveView('main');
        setNavValue(0);
        // Принудительно грузим ленту
        loadData(0);
    };

    // ИСПРАВЛЕНИЕ №2 и №3: Полная очистка состояния при выходе
    const logout = () => {
        localStorage.removeItem('jwt_token');
        setUser(null);
        setUsername('');
        setNavValue(0);
        setActiveView('main');
        setInnerTab(0);

        // Чистим формы логина
        setAuthLogin('');
        setAuthPass('');

        // Чистим формы опроса
        setNewTitle('');
        setNewDesc('');
        setNewOptions(['', '']);
        setIsPublic(true);
        setIsQuadratic(true);
        setCreatedCode(null);
    };

    const refreshUser = async () => {
        if (isAdmin) return;
        try {
            const res = await axios.get(`${API_URL}/wallet/${user.userId}`);
            setUser(res.data);
        } catch (e) { console.error(e); }
    };

    const loadData = async (tabIndex) => {
        if (!user) return;
        try {
            if (tabIndex === 0) {
                const res = await axios.get(`${API_URL}/projects/public`);
                setPublicPolls(res.data);
            } else if (tabIndex === 1) {
                const accessRes = await axios.get(`${API_URL}/projects/access/${user.userId}`);
                setMyUnlockedPolls(accessRes.data);

                const createdRes = await axios.get(`${API_URL}/projects/my/${user.userId}`);
                setMyCreatedPolls(createdRes.data);
                const newStats = {};
                await Promise.all(createdRes.data.map(async (p) => {
                    try {
                        const s = await axios.get(`${API_URL}/history/stats/${p.id}`);
                        newStats[p.id] = s.data;
                    } catch(e){}
                }));
                setStatsMap(newStats);
            } else if (tabIndex === 2) {
                const res = await axios.get(`${API_URL}/history/${user.userId}`);
                setHistory(res.data);
            } else if (tabIndex === 3) {
                refreshUser();
            }
        } catch(e) { console.error(e); }
    };

    useEffect(() => {
        if (user && activeView === 'main') loadData(navValue);
    },[navValue, activeView, innerTab]);

    const loadExplorer = async () => {
        setActiveView('explorer');
        try {
            const res = await axios.get(`${API_URL}/blockchain/explorer`);
            setExplorerData(res.data);
        } catch(e) { alert("Ошибка загрузки блокчейна"); }
    }

    const handleUnlock = async () => {
        if (!searchCode) return;
        try {
            const res = await axios.post(`${API_URL}/projects/unlock?userId=${user.userId}&code=${searchCode.toUpperCase()}`);
            alert(`Опрос "${res.data.title}" разблокирован!`);
            setSearchCode('');
            loadData(1);
        } catch (e) { alert("Код не найден"); }
    };

    const handleCreateSubmit = async () => {
        try {
            const filtered = newOptions.filter(o => o.trim() !== '');
            if (filtered.length < 2) return alert("Минимум 2 варианта");
            const res = await axios.post(`${API_URL}/projects`, {
                title: newTitle, description: newDesc, creatorId: user.userId,
                options: filtered, isPublic: isPublic, voteType: isQuadratic ? 'QUADRATIC' : 'LINEAR'
            });
            setCreatedCode(res.data.accessCode);

            setNewTitle('');
            setNewDesc('');
            setNewOptions(['', '']);
            setIsPublic(true);
            setIsQuadratic(true);

            setTimeout(refreshUser, 1000);
        } catch (e) { alert("Ошибка создания: " + (e.response?.data || "")); }
    };

    const handleVoteCheck = async (poll) => {
        try {
            const res = await axios.get(`${API_URL}/votes/check?userId=${user.userId}&pollId=${poll.id}`);
            if (res.data) alert("Вы уже голосовали!");
            else { setActivePoll(poll); setVotes({}); setActiveView('voting'); }
        } catch(e) { alert("Ошибка проверки"); }
    };

    const handleVoteChange = (optionId, val) => {
        if (activePoll.voteType === 'LINEAR') {
            setVotes({ ...votes, [optionId]: val ? 1 : 0 });
            return;
        }
        const current = votes[optionId] || 0;
        const diff = (val * val) - (current * current);
        const used = Object.values(votes).reduce((sum, v) => sum + v*v, 0);
        if (used + diff <= appConfig.maxBudget) setVotes({ ...votes, [optionId]: val });
    };

    const submitVotes = async () => {
        try {
            await axios.post(`${API_URL}/votes/batch`, { userId: user.userId, pollId: activePoll.id, votes });
            alert("Голоса приняты в обработку Блокчейном!");
            setActiveView('main');
            setVotes({});
            setTimeout(refreshUser, 2000);
        } catch (e) { alert(e.response?.data || "Ошибка сети"); }
    };

    // ==========================================
    //                  VIEWS
    // ==========================================

    if (isAppLoading) return <Box display="flex" justifyContent="center" alignItems="center" height="100vh"><CircularProgress /></Box>;

    if (!user) {
        return (
            <Container maxWidth="xs" sx={{ height: '100vh', display: 'flex', alignItems: 'center' }}>
                <motion.div initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ duration: 0.5 }} style={{ width: '100%' }}>
                    <Paper elevation={6} sx={{ p: 4, borderRadius: 4, textAlign: 'center', bgcolor: 'background.paper' }}>
                        <Typography variant="h4" fontWeight="bold" color="primary" gutterBottom>QV System</Typography>
                        <Tabs value={authMode} onChange={(e, v) => setAuthMode(v)} variant="fullWidth" sx={{ mb: 3 }}>
                            <Tab label="Вход" value="login" />
                            <Tab label="Регистрация" value="register" />
                        </Tabs>
                        <TextField fullWidth label="Логин" variant="outlined" margin="normal" value={authLogin} onChange={e => setAuthLogin(e.target.value)} />
                        <TextField fullWidth label="Пароль" type="password" variant="outlined" margin="normal" value={authPass} onChange={e => setAuthPass(e.target.value)} />
                        <Button fullWidth variant="contained" size="large" sx={{ mt: 3, borderRadius: 2 }} onClick={handleWebAuth} disabled={!authLogin || !authPass}>
                            {authMode === 'login' ? 'Войти' : 'Создать аккаунт'}
                        </Button>
                        <Typography variant="caption" display="block" mt={2} color="text.disabled">
                            Admin access: admin / admin
                        </Typography>
                    </Paper>
                </motion.div>
            </Container>
        );
    }

    if (activeView === 'admin') {
        return (
            <Box sx={{height: '100vh', display: 'flex', flexDirection: 'column'}}>
                <AppBar position="static" color="primary">
                    <Toolbar>
                        <AdminPanelSettingsIcon sx={{mr: 2}} />
                        <Typography variant="h6" sx={{flexGrow: 1}}>Admin Panel</Typography>
                        <IconButton color="inherit" onClick={logout}><LogoutIcon /></IconButton>
                    </Toolbar>
                    <Tabs value={adminTab} onChange={(e, v) => setAdminTab(v)} textColor="inherit" indicatorColor="secondary" variant="fullWidth">
                        <Tab icon={<DashboardIcon />} label="System" />
                        <Tab icon={<ApiIcon />} label="API Docs" />
                    </Tabs>
                </AppBar>
                <Box sx={{flexGrow: 1, overflow: 'auto', p: 0}}>
                    {adminTab === 0 && (
                        <Container sx={{mt: 3}}>
                            <Typography variant="h5" gutterBottom>Статус системы</Typography>
                            <Grid container spacing={2}>
                                <Grid item xs={12} md={6}>
                                    <Card sx={{bgcolor: 'rgba(33, 150, 243, 0.1)'}}><CardContent><Typography color="textSecondary">Микросервисы</Typography><Typography variant="h4">Online</Typography></CardContent></Card>
                                </Grid>
                                <Grid item xs={12} md={6}>
                                    <Card sx={{bgcolor: 'rgba(76, 175, 80, 0.1)'}}><CardContent><Typography color="textSecondary">Блокчейн</Typography><Typography variant="h4">Geth PoA</Typography></CardContent></Card>
                                </Grid>
                            </Grid>
                        </Container>
                    )}
                    {adminTab === 1 && <iframe src="/webjars/swagger-ui/index.html" style={{width: '100%', height: '100%', border: 'none'}} title="API Docs" />}
                </Box>
            </Box>
        );
    }

    if (activeView === 'creator') {
        if (createdCode) {
            return (
                <Container maxWidth="sm" sx={{ py: 4, textAlign: 'center' }}>
                    <PageWrapper>
                        <Typography variant="h5" color="success.main" gutterBottom>Опрос создан!</Typography>
                        <Typography color="text.secondary">Код для доступа:</Typography>
                        <Card sx={{ my: 4, bgcolor: 'rgba(144, 202, 249, 0.1)', border: '1px dashed #90caf9' }}>
                            <CardContent>
                                <Typography variant="h3" sx={{ fontWeight: 'bold', letterSpacing: 6, color: '#90caf9' }}>{createdCode}</Typography>
                            </CardContent>
                        </Card>
                        <Button variant="contained" fullWidth onClick={() => { setCreatedCode(null); setActiveView('main'); loadData(1); }}>Вернуться</Button>
                    </PageWrapper>
                </Container>
            );
        }
        return (
            <Container maxWidth="sm" sx={{pt: 2, pb: 10}}>
                <PageWrapper>
                    <Box display="flex" alignItems="center" mb={2}>
                        <IconButton onClick={() => setActiveView('main')}><ArrowBackIcon /></IconButton>
                        <Typography variant="h6" fontWeight="bold">Создать Опрос</Typography>
                    </Box>
                    <TextField label="Заголовок" fullWidth value={newTitle} onChange={e => setNewTitle(e.target.value)} sx={{mb: 2}}/>
                    <TextField label="Описание" fullWidth multiline rows={2} value={newDesc} onChange={e => setNewDesc(e.target.value)} sx={{mb: 2}}/>

                    <Paper variant="outlined" sx={{ p: 2, mb: 2, bgcolor: 'background.paper', borderRadius: 2 }}>
                        <FormControlLabel control={<Switch checked={isPublic} onChange={e => setIsPublic(e.target.checked)} color="primary" />} label="Публичный опрос (Глобальная лента)" sx={{ display: 'block', mb: 1 }}/>
                        <FormControlLabel control={<Switch checked={isQuadratic} onChange={e => setIsQuadratic(e.target.checked)} color="secondary" />} label={isQuadratic ? `Квадратичное (Лимит: ${appConfig.maxBudget} QV)` : "Обычное (1 человек = 1 голос)"} sx={{ display: 'block' }}/>
                    </Paper>

                    <Typography variant="subtitle2" mt={2} mb={1}>Варианты ответов:</Typography>
                    {newOptions.map((opt, i) => (
                        <TextField key={i} size="small" placeholder={`Вариант ${i+1}`} fullWidth value={opt} onChange={e => { const a=[...newOptions]; a[i]=e.target.value; setNewOptions(a); }} sx={{mb: 1}}/>
                    ))}
                    <Button startIcon={<AddCircleOutlineIcon />} onClick={() => setNewOptions([...newOptions, ''])}>Добавить вариант</Button>

                    <Button variant="contained" size="large" fullWidth onClick={handleCreateSubmit} disabled={!newTitle} sx={{ mt: 3, borderRadius: 3 }}>
                        Опубликовать (-{appConfig.pollCost} QV)
                    </Button>
                </PageWrapper>
            </Container>
        );
    }

    if (activeView === 'voting' && activePoll) {
        const used = activePoll.voteType === 'LINEAR'
            ? Object.values(votes).reduce((sum, v) => sum + Number(v), 0)
            : Object.values(votes).reduce((sum, v) => sum + v*v, 0);

        const budgetLimit = activePoll.voteType === 'LINEAR' ? user.balance : appConfig.maxBudget;
        const remaining = budgetLimit - used;

        return (
            <Container maxWidth="sm" sx={{ pb: 10 }}>
                <PageWrapper>
                    <Box display="flex" alignItems="center" my={2}>
                        <IconButton onClick={() => setActiveView('main')}><ArrowBackIcon /></IconButton>
                        <Typography variant="h6" fontWeight="bold" noWrap>{activePoll.title}</Typography>
                    </Box>

                    <Paper elevation={4} sx={{ position: 'sticky', top: 10, zIndex: 100, p: 2, mb: 3, borderRadius: 3, bgcolor: remaining < 0 ? '#b71c1c' : '#1e1e1e' }}>
                        <Box display="flex" justifyContent="space-between" alignItems="center">
                            <Typography variant="subtitle2" color="text.secondary">
                                {activePoll.voteType === 'LINEAR' ? 'Ваш глобальный баланс:' : 'Бюджет опроса (QV):'}
                            </Typography>
                            <Typography variant="h5" fontWeight="bold" color={remaining < 0 ? 'error' : 'primary.light'}>
                                {remaining} {activePoll.voteType === 'LINEAR' ? 'QV' : `/ ${appConfig.maxBudget}`}
                            </Typography>
                        </Box>
                    </Paper>

                    <Stack spacing={2}>
                        {activePoll.options.map(opt => {
                            const val = votes[opt.id] || 0;
                            const cost = activePoll.voteType === 'LINEAR' ? val : val*val;
                            return (
                                <Card key={opt.id} variant="outlined" sx={{ borderRadius: 3 }}>
                                    <CardContent>
                                        <Typography variant="subtitle1" fontWeight="500">{opt.text}</Typography>
                                        {activePoll.voteType === 'QUADRATIC' ? (
                                            <Box display="flex" alignItems="center" gap={2} mt={1}>
                                                <Slider value={val} min={0} max={10} step={1} marks onChange={(e, v) => handleVoteChange(opt.id, v)} sx={{ flexGrow: 1 }} />
                                                <Typography variant="h6" sx={{minWidth: 20, textAlign: 'center'}}>{val}</Typography>
                                            </Box>
                                        ) : (
                                            <Box display="flex" alignItems="center" gap={2} mt={1}>
                                                <FormControlLabel control={<Checkbox checked={val === 1} onChange={(e) => handleVoteChange(opt.id, e.target.checked ? 1 : 0)} />} label="Отдать голос" />
                                            </Box>
                                        )}
                                        <Typography variant="caption" color="text.secondary" display="block" align="right" mt={1}>
                                            Стоимость: <b>{cost}</b> QV
                                        </Typography>
                                    </CardContent>
                                </Card>
                            );
                        })}
                    </Stack>

                    <Box position="fixed" bottom={0} left={0} right={0} p={2} bgcolor="background.default" sx={{ borderTop: '1px solid rgba(255,255,255,0.1)', zIndex: 1000 }}>
                        <Container maxWidth="sm">
                            <Button variant="contained" fullWidth size="large" disabled={used === 0 || remaining < 0} onClick={submitVotes} sx={{ borderRadius: 3 }}>
                                Подтвердить выбор ({used} QV)
                            </Button>
                        </Container>
                    </Box>
                </PageWrapper>
            </Container>
        );
    }

    if (activeView === 'explorer') {
        return (
            <Container maxWidth="sm" sx={{mt: 2, pb: 5}}>
                <PageWrapper>
                    <Box display="flex" alignItems="center" mb={2}>
                        <IconButton onClick={() => setActiveView('main')}><ArrowBackIcon /></IconButton>
                        <Typography variant="h6" fontWeight="bold">Глобальный Реестр</Typography>
                    </Box>
                    {explorerData.length === 0 && <CircularProgress sx={{display:'block', mx:'auto', mt:5}}/>}
                    {explorerData.map((tx, i) => (
                        <Paper key={i} sx={{ p: 2, mb: 2, bgcolor: '#000', color: '#0f0', fontFamily: 'monospace', overflowX: 'auto', borderRadius: 2 }}>
                            <Typography variant="caption" color="#fff">Block: {tx.blockNumber}</Typography><br/>
                            <Typography variant="caption" color="#aaa">Hash: {tx.txHash}</Typography><br/>
                            <Typography mt={1}>{tx.data}</Typography>
                        </Paper>
                    ))}
                </PageWrapper>
            </Container>
        );
    }

    // --- MAIN RENDER (WITH BOTTOM NAV) ---
    const renderContent = () => {
        switch (navValue) {
            case 0:
                return (
                    <PageWrapper>
                        <Typography variant="h5" fontWeight="bold" sx={{ p: 2 }}>Глобальная Лента</Typography>
                        {publicPolls.length === 0 && <Typography align="center" color="text.secondary" mt={5}>Опросов пока нет</Typography>}
                        <Box px={2}>
                            {publicPolls.map(poll => (
                                <Card key={poll.id} elevation={2} sx={{ borderRadius: 3, mb: 2 }}>
                                    <CardContent>
                                        <Box display="flex" justifyContent="space-between" mb={1}>
                                            <Typography variant="h6">{poll.title}</Typography>
                                            <Chip label={poll.voteType === 'QUADRATIC' ? 'QV' : 'Linear'} size="small" color={poll.voteType === 'QUADRATIC' ? 'secondary' : 'default'} />
                                        </Box>
                                        <Typography variant="body2" color="text.secondary" paragraph>{poll.description}</Typography>
                                        <Button variant="outlined" fullWidth onClick={() => handleVoteCheck(poll)}>Участвовать</Button>
                                    </CardContent>
                                </Card>
                            ))}
                        </Box>
                    </PageWrapper>
                );
            case 1:
                return (
                    <PageWrapper>
                        <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
                            <Tabs value={innerTab} onChange={(e, v) => setInnerTab(v)} variant="fullWidth">
                                <Tab label="Доступные мне" />
                                <Tab label="Мои опросы" />
                            </Tabs>
                        </Box>

                        {innerTab === 0 && (
                            <Box px={2}>
                                <Paper sx={{ p: 1, mb: 2, borderRadius: 2, display: 'flex', gap: 1 }}>
                                    <TextField fullWidth size="small" placeholder="Код закрытого опроса..." value={searchCode} onChange={e => setSearchCode(e.target.value)} />
                                    <Button variant="contained" onClick={handleUnlock}>Ввод</Button>
                                </Paper>
                                {myUnlockedPolls.length === 0 && <Typography align="center" color="text.secondary" mt={2}>Нет приватных опросов</Typography>}
                                {myUnlockedPolls.map(poll => (
                                    <Card key={poll.id} sx={{ borderRadius: 3, mb: 2 }}>
                                        <CardContent>
                                            <Typography variant="h6">{poll.title}</Typography>
                                            <Button variant="contained" fullWidth sx={{mt: 2}} onClick={() => handleVoteCheck(poll)}>Открыть</Button>
                                        </CardContent>
                                    </Card>
                                ))}
                            </Box>
                        )}

                        {innerTab === 1 && (
                            <Box px={2}>
                                <Button variant="contained" color="secondary" startIcon={<AddCircleOutlineIcon/>} fullWidth sx={{ borderRadius: 3, py: 1.5, mb: 2 }} onClick={() => setActiveView('creator')}>
                                    Создать новый
                                </Button>
                                {myCreatedPolls.map(p => {
                                    const stat = statsMap[p.id];
                                    return (
                                        <Card key={p.id} sx={{ borderRadius: 3, mb: 2 }}>
                                            <CardContent>
                                                <Box display="flex" justifyContent="space-between" mb={1}>
                                                    <Typography variant="h6" fontWeight="bold">{p.title}</Typography>
                                                    {!p.isPublic && <Chip label={p.accessCode} color="primary" variant="outlined" onClick={() => {navigator.clipboard.writeText(p.accessCode); alert("Код скопирован");}} icon={<ContentCopyIcon sx={{fontSize:16}}/>} clickable size="small"/>}
                                                </Box>
                                                <Divider sx={{ my: 1.5 }} />
                                                {stat ? (
                                                    <Grid container spacing={2} textAlign="center">
                                                        <Grid item xs={6}>
                                                            <Box bgcolor="rgba(255,255,255,0.05)" p={1} borderRadius={2}>
                                                                <GroupIcon color="primary" fontSize="small"/>
                                                                <Typography variant="h6">{stat.participants}</Typography>
                                                                <Typography variant="caption" color="text.secondary">Людей</Typography>
                                                            </Box>
                                                        </Grid>
                                                        <Grid item xs={6}>
                                                            <Box bgcolor="rgba(255,255,255,0.05)" p={1} borderRadius={2}>
                                                                <EqualizerIcon color="success" fontSize="small"/>
                                                                <Typography variant="h6">{stat.totalVotes}</Typography>
                                                                <Typography variant="caption" color="text.secondary">QV потрачено</Typography>
                                                            </Box>
                                                        </Grid>
                                                        <Grid item xs={12}>
                                                            <Box textAlign="left" mt={1}>
                                                                {p.options && p.options.map(opt => {
                                                                    const v = stat.optionStats?.[opt.id] || 0;
                                                                    const pct = stat.totalVotes > 0 ? Math.round((v/stat.totalVotes)*100) : 0;
                                                                    return (
                                                                        <Box key={opt.id} mb={1}>
                                                                            <Box display="flex" justifyContent="space-between" mb={0.5}>
                                                                                <Typography variant="body2">{opt.text}</Typography>
                                                                                <Typography variant="body2" color="primary">{v} <span style={{color: 'gray'}}>({pct}%)</span></Typography>
                                                                            </Box>
                                                                            <div style={{width:'100%', height:6, background:'#333', borderRadius:3}}>
                                                                                <div style={{width:`${pct}%`, height:'100%', background:'#90caf9', borderRadius:3, transition: 'width 0.5s ease-in-out'}}></div>
                                                                            </div>
                                                                        </Box>
                                                                    )
                                                                })}
                                                            </Box>
                                                        </Grid>
                                                    </Grid>
                                                ) : <CircularProgress size={20} sx={{display:'block', mx:'auto'}}/>}
                                            </CardContent>
                                        </Card>
                                    );
                                })}
                            </Box>
                        )}
                    </PageWrapper>
                );
            case 2: // История
                const grouped = getGroupedHistory();
                return (
                    <PageWrapper>
                        <Box display="flex" justifyContent="space-between" alignItems="center" p={2}>
                            <Typography variant="h5" fontWeight="bold">Мой Блокчейн-лог</Typography>
                            <Button variant="outlined" size="small" startIcon={<CodeIcon/>} onClick={loadExplorer}>Блокчейн</Button>
                        </Box>
                        <Box px={2}>
                            {grouped.length === 0 && <Typography align="center" color="text.secondary" mt={4}>История пуста</Typography>}
                            {grouped.map((g, i) => (
                                <Paper key={i} sx={{ mb: 2, overflow: 'hidden', borderRadius: 3 }}>
                                    <Box bgcolor="rgba(144, 202, 249, 0.1)" p={2} display="flex" justifyContent="space-between" alignItems="center">
                                        <Typography fontWeight="bold">{g.title}</Typography>
                                        <Chip label={`${Number(g.totalCost).toFixed(0)} QV`} size="small" color="primary"/>
                                    </Box>
                                    <List dense>
                                        {g.items.map((h, k) => (
                                            <div key={k}>
                                                <ListItem>
                                                    <ListItemText
                                                        primary={h.optionText}
                                                        secondary={
                                                            <>
                                                                <Typography component="span" variant="body2" color="text.primary">Голосов: {h.voteCount}</Typography>
                                                                <br/>
                                                                <Typography component="span" variant="caption" sx={{ fontFamily: 'monospace', color: '#666' }}>
                                                                    Tx: {h.txHash ? h.txHash.substring(0,10)+'...' : 'Обработка...'}
                                                                </Typography>
                                                            </>
                                                        }
                                                    />
                                                </ListItem>
                                                {k < g.items.length - 1 && <Divider component="li" />}
                                            </div>
                                        ))}
                                    </List>
                                </Paper>
                            ))}
                        </Box>
                    </PageWrapper>
                );
            case 3: // Профиль
                return (
                    <PageWrapper>
                        <Container sx={{ textAlign: 'center', pt: 5 }}>
                            <Box sx={{ width: 80, height: 80, borderRadius: '50%', bgcolor: 'primary.main', display: 'flex', alignItems: 'center', justifyContent: 'center', mx: 'auto', mb: 2 }}>
                                <PersonIcon sx={{ fontSize: 40, color: '#fff' }} />
                            </Box>
                            <Typography variant="h5" fontWeight="bold">{username}</Typography>
                            <Typography color="text.secondary" mb={4}>ID: {user?.userId}</Typography>

                            <Paper elevation={4} sx={{ p: 3, borderRadius: 4, mb: 4, background: 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)', color: 'white' }}>
                                <Typography variant="subtitle2" sx={{ opacity: 0.8 }}>ГЛОБАЛЬНЫЙ БАЛАНС</Typography>
                                <Typography variant="h2" fontWeight="bold" sx={{ my: 1 }}>{user?.balance}</Typography>
                                <Typography variant="caption">Обновляется ежемесячно</Typography>
                            </Paper>

                            <Button variant="outlined" color="error" fullWidth size="large" onClick={logout} sx={{ borderRadius: 3 }}>
                                Выйти из аккаунта
                            </Button>
                        </Container>
                    </PageWrapper>
                );
            default: return null;
        }
    };

    if (activeView === 'main') {
        return (
            <Box sx={{ pb: 7, bgcolor: 'background.default', minHeight: '100vh' }}>
                <AnimatePresence mode="wait">
                    {renderContent()}
                </AnimatePresence>

                <Paper sx={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: 1000 }} elevation={8}>
                    <BottomNavigation
                        showLabels
                        value={navValue}
                        onChange={(event, newValue) => {
                            setNavValue(newValue);
                            window.scrollTo(0, 0);
                        }}
                        sx={{ height: 65 }}
                    >
                        <BottomNavigationAction label="Лента" icon={<PublicIcon />} />
                        <BottomNavigationAction label="Опросы" icon={<AssessmentIcon />} />
                        <BottomNavigationAction label="История" icon={<HistoryIcon />} />
                        <BottomNavigationAction label="Профиль" icon={<PersonIcon />} />
                    </BottomNavigation>
                </Paper>
            </Box>
        );
    }
}

export default App;