import { useState, useEffect } from 'react';
import axios from 'axios';
import {
    Container, Typography, Card, CardContent, Slider, Button,
    List, ListItem, ListItemText, Divider, Chip, TextField, Box,
    IconButton, CircularProgress, Paper, AppBar, Toolbar, Stack, Grid,
    Tabs, Tab
} from '@mui/material';

import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import AssessmentIcon from '@mui/icons-material/Assessment';
import HistoryIcon from '@mui/icons-material/History';
import HowToVoteIcon from '@mui/icons-material/HowToVote';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import GroupIcon from '@mui/icons-material/Group';
import EqualizerIcon from '@mui/icons-material/Equalizer';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import ApiIcon from '@mui/icons-material/Api';
import DashboardIcon from '@mui/icons-material/Dashboard';
import LogoutIcon from '@mui/icons-material/Logout';

const API_URL = '/api';
const MAX_BUDGET = 100;
const ADMIN_ID = 0;

function App() {
    const [view, setView] = useState('loading');
    const [user, setUser] = useState(null);
    const [isAdmin, setIsAdmin] = useState(false);
    const [username, setUsername] = useState('');

    const [myUnlockedPolls, setMyUnlockedPolls] = useState([]);
    const [myCreatedPolls, setMyCreatedPolls] = useState([]);
    const [statsMap, setStatsMap] = useState({});
    const [history, setHistory] = useState([]);

    const [activePoll, setActivePoll] = useState(null);
    const [adminTab, setAdminTab] = useState(0);

    const [loginId, setLoginId] = useState('');
    const [newTitle, setNewTitle] = useState('');
    const [newDesc, setNewDesc] = useState('');
    const [newOptions, setNewOptions] = useState(['', '']);
    const [createdCode, setCreatedCode] = useState(null);
    const [searchCode, setSearchCode] = useState('');
    const [votes, setVotes] = useState({});

    const [loginUsername, setLoginUsername] = useState('');
    const [loginPassword, setLoginPassword] = useState('');

    useEffect(() => {
        const initApp = async () => {
            if (window.Telegram?.WebApp?.initDataUnsafe?.user) {
                const tgUser = window.Telegram.WebApp.initDataUnsafe.user;
                setUsername(tgUser.username ? `@${tgUser.username}` : tgUser.first_name);
                window.Telegram.WebApp.expand();
                performTgLogin(tgUser.id);
            } else {
                setView('login');
            }
        };
        initApp();
    }, []);

    const performTgLogin = async (id) => {
        try {
            const res = await axios.post(`${API_URL}/auth/login/${id}`);
            completeLogin(res.data);
        } catch (e) {
            alert("Ошибка TG входа");
        }
    };

    const performWebLogin = async () => {
        if (loginUsername === 'admin' && loginPassword === 'admin') {
            setIsAdmin(true);
            setUser({ userId: 0, balance: '∞' });
            setView('admin');
            return;
        }

        try {
            const res = await axios.post(`${API_URL}/auth/login/web`, {
                username: loginUsername,
                password: loginPassword
            });
            completeLogin(res.data);
        } catch (e) {
            alert("Неверный логин или пароль");
        }
    };

    const completeLogin = (userData) => {
        setUser(userData);
        if (!username) setUsername(`User ${userData.userId}`);
        setIsAdmin(false);
        setView('menu');
    };

    const performLogin = async (id) => {
        if (Number(id) === ADMIN_ID) {
            setIsAdmin(true);
            setUser({ userId: ADMIN_ID, balance: '∞' });
            setView('admin');
            return;
        }

        try {
            const res = await axios.post(`${API_URL}/auth/login/${id}`);
            setUser(res.data);
            if (!username) setUsername(`User ${res.data.userId}`);
            setIsAdmin(false);
            setView('menu');
        } catch (e) {
            alert("Ошибка входа: " + e.message);
            setView('login');
        }
    };

    // --- HELPERS ---
    const refreshUser = async () => {
        if (isAdmin) return;
        const res = await axios.post(`${API_URL}/auth/login/${user.userId}`);
        setUser(res.data);
    };

    const getGroupedHistory = () => {
        const groups = {};
        history.forEach(item => {
            const key = item.pollId || 'unknown';
            if (!groups[key]) groups[key] = { title: item.pollTitle || `Poll #${key}`, totalCost: 0, items: [] };
            groups[key].items.push(item);
            groups[key].totalCost += Number(item.cost || 0);
        });
        return Object.values(groups).reverse();
    };

    // --- ACTIONS ---
    const loadData = async (type) => {
        try {
            if (type === 'access') {
                const res = await axios.get(`${API_URL}/projects/access/${user.userId}`);
                setMyUnlockedPolls(res.data);
            } else if (type === 'created') {
                const res = await axios.get(`${API_URL}/projects/my/${user.userId}`);
                setMyCreatedPolls(res.data);
                // Load stats
                const newStats = {};
                await Promise.all(res.data.map(async (p) => {
                    try {
                        const s = await axios.get(`${API_URL}/history/stats/${p.id}`);
                        newStats[p.id] = s.data;
                    } catch(e){}
                }));
                setStatsMap(newStats);
            } else if (type === 'history') {
                const res = await axios.get(`${API_URL}/history/${user.userId}`);
                setHistory(res.data);
            }
        } catch(e) { console.error(e); }
    };

    const handleCreate = async () => {
        try {
            const opts = newOptions.filter(o => o.trim());
            if (opts.length < 2) return alert("Минимум 2 варианта");
            const res = await axios.post(`${API_URL}/projects`, {
                title: newTitle, description: newDesc, creatorId: user.userId, options: opts
            });
            setCreatedCode(res.data.accessCode);
            refreshUser();
        } catch(e) { alert("Ошибка: " + (e.response?.data?.message || "Error")); }
    };

    const handleUnlock = async () => {
        try {
            const res = await axios.post(`${API_URL}/projects/unlock?userId=${user.userId}&code=${searchCode.toUpperCase()}`);
            alert(`Опрос "${res.data.title}" добавлен`);
            setSearchCode(''); setView('my-access'); loadData('access');
        } catch(e) { alert("Код не найден"); }
    };

    const handleVoteCheck = async (poll) => {
        try {
            const res = await axios.get(`${API_URL}/votes/check?userId=${user.userId}&pollId=${poll.id}`);
            if (res.data) alert("Вы уже голосовали!");
            else { setActivePoll(poll); setVotes({}); setView('voting'); }
        } catch(e) { alert("Ошибка проверки"); }
    };

    const submitVotes = async () => {
        try {
            await axios.post(`${API_URL}/votes/batch`, { userId: user.userId, pollId: activePoll.id, votes });
            alert("Принято!"); setView('menu'); setVotes({});
            setTimeout(refreshUser, 2000);
        } catch(e) { alert(e.response?.data || "Error"); }
    };

    // --- VIEWS ---

    if (view === 'loading') return <Box display="flex" justifyContent="center" height="100vh" alignItems="center"><CircularProgress /></Box>;

    if (view === 'login') {
        return (
            <Container maxWidth="xs" sx={{mt: 10}}>
                <Paper elevation={3} sx={{p: 4, textAlign: 'center', borderRadius: 4}}>
                    <Typography variant="h4" fontWeight="bold" color="primary" gutterBottom>QV System</Typography>
                    <Typography color="text.secondary" mb={3}>Вход в систему</Typography>

                    <TextField
                        fullWidth label="Логин" variant="outlined" margin="normal"
                        value={loginUsername} onChange={e => setLoginUsername(e.target.value)}
                    />
                    <TextField
                        fullWidth label="Пароль" type="password" variant="outlined" margin="normal"
                        value={loginPassword} onChange={e => setLoginPassword(e.target.value)}
                    />

                    <Button
                        fullWidth variant="contained" size="large" sx={{mt: 3}}
                        onClick={performWebLogin}
                        disabled={!loginUsername || !loginPassword}
                    >
                        Войти
                    </Button>

                    <Typography variant="caption" display="block" mt={2} color="text.disabled">
                        Demo: user / password <br/>
                        Admin: admin / admin
                    </Typography>
                </Paper>
            </Container>
        );
    }

    // --- ADMIN PANEL ---
    if (view === 'admin') {
        return (
            <Box sx={{height: '100vh', display: 'flex', flexDirection: 'column'}}>
                <AppBar position="static" color="primary">
                    <Toolbar>
                        <AdminPanelSettingsIcon sx={{mr: 2}} />
                        <Typography variant="h6" sx={{flexGrow: 1}}>Панель Администратора</Typography>
                        <IconButton color="inherit" onClick={() => setView('login')}><LogoutIcon /></IconButton>
                    </Toolbar>
                    <Tabs value={adminTab} onChange={(e, v) => setAdminTab(v)} textColor="inherit" indicatorColor="secondary" variant="fullWidth">
                        <Tab icon={<DashboardIcon />} label="Инфо" />
                        <Tab icon={<ApiIcon />} label="API Docs (Swagger)" />
                    </Tabs>
                </AppBar>

                <Box sx={{flexGrow: 1, overflow: 'auto', p: 0}}>
                    {adminTab === 0 && (
                        <Container sx={{mt: 3}}>
                            <Typography variant="h5" gutterBottom>Статус системы</Typography>
                            <Grid container spacing={2}>
                                <Grid item xs={12} md={6}>
                                    <Card sx={{bgcolor: '#e3f2fd'}}>
                                        <CardContent>
                                            <Typography color="textSecondary">Микросервисы</Typography>
                                            <Typography variant="h4">4 Активны</Typography>
                                            <Typography variant="body2">Voting, Wallet, History, Blockchain</Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                                <Grid item xs={12} md={6}>
                                    <Card sx={{bgcolor: '#fff3e0'}}>
                                        <CardContent>
                                            <Typography color="textSecondary">Блокчейн</Typography>
                                            <Typography variant="h4">Geth PoW</Typography>
                                            <Typography variant="body2">Mining Active</Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                            </Grid>

                            <Typography variant="h6" mt={4} gutterBottom>Быстрые действия</Typography>
                            <Button variant="outlined" href="http://localhost:8761" target="_blank">Eureka</Button>
                            <Button variant="outlined" href="http://localhost:15672" target="_blank" sx={{ml: 1}}>RabbitMQ</Button>
                            <Button variant="outlined" href="http://localhost:9411" target="_blank" sx={{ml: 1}}>Zipkin</Button>
                        </Container>
                    )}

                    {adminTab === 1 && (
                        <iframe
                            src="/webjars/swagger-ui/index.html"
                            style={{width: '100%', height: '100%', border: 'none'}}
                            title="API Docs"
                        />
                    )}
                </Box>
            </Box>
        );
    }

    // --- USER MENU ---
    if (view === 'menu') {
        return (
            <Container maxWidth="sm" sx={{ py: 4 }}>
                <Box textAlign="center" mb={4}>
                    <Typography variant="h4" fontWeight="bold">QV Voting</Typography>
                    <Typography color="text.secondary">Привет, {username}!</Typography>
                    <Paper elevation={3} sx={{ mt: 2, p: 2, background: 'linear-gradient(45deg, #2196F3, #21CBF3)', color: 'white', borderRadius: 3 }}>
                        <Typography variant="caption">БАЛАНС</Typography>
                        <Typography variant="h3" fontWeight="bold">{user?.balance}</Typography>
                        <Typography variant="subtitle2">Global Credits</Typography>
                    </Paper>
                </Box>

                <Stack spacing={2}>
                    <Paper sx={{ p: 2, borderRadius: 3, border: '1px solid #eee' }}>
                        <Box display="flex" gap={1}>
                            <TextField fullWidth size="small" placeholder="Код опроса" value={searchCode} onChange={e => setSearchCode(e.target.value)} />
                            <Button variant="contained" onClick={handleUnlock}>Ввод</Button>
                        </Box>
                    </Paper>
                    <Button variant="contained" size="large" startIcon={<AddCircleOutlineIcon />} onClick={() => { setCreatedCode(null); setView('creator'); }}>
                        Создать Опрос <Chip label="-50 QV" size="small" sx={{ml: 1, bgcolor: 'rgba(255,255,255,0.2)', color: 'white'}}/>
                    </Button>
                    <Grid container spacing={2}>
                        <Grid item xs={6}>
                            <Button variant="outlined" fullWidth size="large" startIcon={<HowToVoteIcon />} onClick={() => { setView('my-access'); loadData('access'); }}>
                                Голосовать
                            </Button>
                        </Grid>
                        <Grid item xs={6}>
                            <Button variant="outlined" fullWidth size="large" startIcon={<AssessmentIcon />} onClick={() => { setView('my-created'); loadData('created'); }}>
                                Мои опросы
                            </Button>
                        </Grid>
                    </Grid>
                    <Button startIcon={<HistoryIcon />} color="secondary" onClick={() => { setView('history'); loadData('history'); }}>История</Button>
                    <Button color="inherit" onClick={() => setView('login')} startIcon={<LogoutIcon/>}>Выход</Button>
                </Stack>
            </Container>
        );
    }

    // ... (Экраны CREATOR, VOTER, MY-CREATED, HISTORY остаются без изменений, скопируй их из прошлого ответа) ...
    // Если нужно, я продублирую, но они не менялись.

    // --- CREATOR ---
    if (view === 'creator') {
        if (createdCode) {
            return (
                <Container maxWidth="sm" sx={{ py: 4, textAlign: 'center' }}>
                    <Typography variant="h5" color="success.main">Опрос создан!</Typography>
                    <Card sx={{ my: 4, bgcolor: '#e3f2fd' }}><CardContent><Typography variant="h3" sx={{ fontWeight: 'bold', letterSpacing: 4 }}>{createdCode}</Typography></CardContent></Card>
                    <Button variant="contained" fullWidth onClick={() => setView('menu')}>В меню</Button>
                </Container>
            );
        }
        return (
            <Container maxWidth="sm" sx={{mt: 2}}>
                <AppBar position="static" color="transparent" elevation={0}><Toolbar><IconButton onClick={() => setView('menu')}><ArrowBackIcon /></IconButton><Typography variant="h6">Новый опрос</Typography></Toolbar></AppBar>
                <Stack spacing={2} mt={2}>
                    <TextField label="Заголовок" fullWidth value={newTitle} onChange={e => setNewTitle(e.target.value)} />
                    <TextField label="Описание" fullWidth multiline rows={3} value={newDesc} onChange={e => setNewDesc(e.target.value)} />
                    {newOptions.map((opt, i) => (
                        <TextField key={i} size="small" placeholder={`Вариант ${i+1}`} fullWidth value={opt} onChange={e => { const a=[...newOptions]; a[i]=e.target.value; setNewOptions(a); }} />
                    ))}
                    <Button startIcon={<AddCircleOutlineIcon />} onClick={() => setNewOptions([...newOptions, ''])}>Добавить вариант</Button>
                    <Button variant="contained" size="large" onClick={handleCreate} disabled={!newTitle}>Создать</Button>
                </Stack>
            </Container>
        );
    }

    // --- VOTING LIST ---
    if (view === 'my-access') {
        return (
            <Container maxWidth="sm">
                <AppBar position="static" color="transparent" elevation={0}><Toolbar><IconButton onClick={() => setView('menu')}><ArrowBackIcon /></IconButton><Typography variant="h6">Доступные мне</Typography></Toolbar></AppBar>
                <Stack spacing={2} mt={2}>
                    {myUnlockedPolls.map(poll => (
                        <Card key={poll.id} elevation={2}><CardContent>
                            <Typography variant="h6">{poll.title}</Typography>
                            <Typography variant="body2" color="text.secondary" paragraph>{poll.description}</Typography>
                            <Button variant="contained" fullWidth onClick={() => handleVoteCheck(poll)}>Открыть голосование</Button>
                        </CardContent></Card>
                    ))}
                </Stack>
            </Container>
        );
    }

    // --- VOTING ---
    if (view === 'voting' && activePoll) {
        const used = Object.values(votes).reduce((sum, v) => sum + v*v, 0);
        const remaining = MAX_BUDGET - used;
        return (
            <Container maxWidth="sm" sx={{ pb: 10 }}>
                <AppBar position="static" color="transparent" elevation={0}><Toolbar><IconButton onClick={() => setView('my-access')}><ArrowBackIcon /></IconButton><Typography variant="h6">{activePoll.title}</Typography></Toolbar></AppBar>
                <Paper elevation={4} sx={{ position: 'sticky', top: 10, zIndex: 100, p: 2, mb: 3, borderRadius: 3, bgcolor: remaining < 0 ? '#ffebee' : '#e8f5e9' }}>
                    <Box display="flex" justifyContent="space-between"><Typography>Бюджет:</Typography><Typography variant="h6" color={remaining < 0 ? 'error' : 'success.main'}>{remaining} / 100</Typography></Box>
                </Paper>
                <Stack spacing={2}>
                    {activePoll.options.map(opt => {
                        const val = votes[opt.id] || 0;
                        return (
                            <Card key={opt.id} variant="outlined"><CardContent>
                                <Typography variant="subtitle1">{opt.text}</Typography>
                                <Box display="flex" alignItems="center" gap={2}><Slider value={val} min={0} max={10} step={1} marks onChange={(e, v) => { const diff = v*v - val*val; if (used + diff <= MAX_BUDGET) setVotes({...votes, [opt.id]: v}); }} sx={{ flexGrow: 1 }} /><Typography variant="h6">{val}</Typography></Box>
                                <Typography variant="caption" color="text.secondary">Цена: <b>{val*val}</b> QV</Typography>
                            </CardContent></Card>
                        );
                    })}
                </Stack>
                <Box position="fixed" bottom={0} left={0} right={0} p={2} bgcolor="white" borderTop="1px solid #eee">
                    <Button variant="contained" fullWidth size="large" disabled={used === 0 || remaining < 0} onClick={submitVotes}>Отправить</Button>
                </Box>
            </Container>
        );
    }

    // --- MY CREATED ---
    if (view === 'my-created') {
        return (
            <Container maxWidth="sm">
                <AppBar position="static" color="transparent" elevation={0}><Toolbar><IconButton onClick={() => setView('menu')}><ArrowBackIcon /></IconButton><Typography variant="h6">Я создал</Typography></Toolbar></AppBar>
                <Stack spacing={2} mt={2}>
                    {myCreatedPolls.map(p => {
                        const stat = statsMap[p.id];
                        return (
                            <Card key={p.id}><CardContent>
                                <Box display="flex" justifyContent="space-between"><Typography variant="h6">{p.title}</Typography><Chip label={p.accessCode} color="primary" onClick={() => {navigator.clipboard.writeText(p.accessCode); alert("Copied")}} icon={<ContentCopyIcon sx={{fontSize:16}}/>} /></Box>
                                <Divider sx={{ my: 2 }} />
                                {stat ? (
                                    <Grid container spacing={2} textAlign="center">
                                        <Grid item xs={6}><Box bgcolor="#e3f2fd" p={1} borderRadius={2}><GroupIcon color="primary"/><Typography variant="caption" display="block">Людей</Typography><Typography variant="h6">{stat.participants}</Typography></Box></Grid>
                                        <Grid item xs={6}><Box bgcolor="#e0f2f1" p={1} borderRadius={2}><EqualizerIcon color="success"/><Typography variant="caption" display="block">Голосов</Typography><Typography variant="h6">{stat.totalVotes}</Typography></Box></Grid>
                                        <Grid item xs={12}><Box textAlign="left" mt={1}>
                                            {p.options && p.options.map(opt => {
                                                const v = stat.optionStats?.[opt.id] || 0;
                                                const pct = stat.totalVotes > 0 ? Math.round((v/stat.totalVotes)*100) : 0;
                                                return (
                                                    <Box key={opt.id} mb={1}><Box display="flex" justifyContent="space-between"><span>{opt.text}</span><b>{v} ({pct}%)</b></Box><div style={{width:'100%', height:6, background:'#eee', borderRadius:3}}><div style={{width:`${pct}%`, height:'100%', background:'#2481cc', borderRadius:3}}></div></div></Box>
                                                )
                                            })}
                                        </Box></Grid>
                                    </Grid>
                                ) : <CircularProgress size={20}/>}
                            </CardContent></Card>
                        );
                    })}
                </Stack>
            </Container>
        );
    }

    // --- HISTORY ---
    if (view === 'history') {
        const grouped = getGroupedHistory();
        return (
            <Container maxWidth="sm">
                <AppBar position="static" color="transparent" elevation={0}><Toolbar><IconButton onClick={() => setView('menu')}><ArrowBackIcon /></IconButton><Typography variant="h6">История</Typography></Toolbar></AppBar>
                {grouped.map((g, i) => (
                    <Paper key={i} sx={{ mb: 2, overflow: 'hidden' }}>
                        <Box bgcolor="#e3f2fd" p={2} display="flex" justifyContent="space-between"><Typography fontWeight="bold">{g.title}</Typography><Chip label={`${g.totalCost.toFixed(0)} QV`} size="small"/></Box>
                        <List dense>{g.items.map((h, k) => (
                            <div key={k}><ListItem><ListItemText primary={h.optionText} secondary={`Голосов: ${h.voteCount} • Tx: ${h.txHash ? h.txHash.substring(0,6)+'...' : '...'}`}/></ListItem><Divider/></div>
                        ))}</List>
                    </Paper>
                ))}
            </Container>
        );
    }
}

export default App;