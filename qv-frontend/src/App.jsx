import { useState, useEffect } from 'react';
import axios from 'axios';
import {
    Container, Typography, Card, CardContent, Slider, Button,
    List, ListItem, ListItemText, Divider, Chip, TextField, Box,
    IconButton, CircularProgress, Paper, AppBar, Toolbar, Stack, Grid
} from '@mui/material';

import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import AssessmentIcon from '@mui/icons-material/Assessment';
import HistoryIcon from '@mui/icons-material/History';
import HowToVoteIcon from '@mui/icons-material/HowToVote';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import GroupIcon from '@mui/icons-material/Group';
import EqualizerIcon from '@mui/icons-material/Equalizer';

const API_URL = '/api';
const MAX_BUDGET = 100;

function App() {
    const [view, setView] = useState('loading');
    const [user, setUser] = useState(null);
    const [username, setUsername] = useState('');

    const [myUnlockedPolls, setMyUnlockedPolls] = useState([]);
    const [myCreatedPolls, setMyCreatedPolls] = useState([]);
    const [statsMap, setStatsMap] = useState({});
    const [activePoll, setActivePoll] = useState(null);
    const [history, setHistory] = useState([]);

    const [newTitle, setNewTitle] = useState('');
    const [newDesc, setNewDesc] = useState('');
    const [newOptions, setNewOptions] = useState(['', '']);
    const [createdCode, setCreatedCode] = useState(null);
    const [searchCode, setSearchCode] = useState('');

    const [votes, setVotes] = useState({});

    const getGroupedHistory = () => {
        const groups = {};
        history.forEach(item => {
            const key = item.pollId;
            if (!groups[key]) {
                groups[key] = {
                    title: item.pollTitle || `Опрос #${item.pollId}`,
                    totalCost: 0,
                    items: []
                };
            }
            groups[key].items.push(item);
            groups[key].totalCost += item.cost;
        });
        return Object.values(groups).reverse();
    };

    useEffect(() => {
        const initApp = async () => {
            let tgUserId = 101;
            if (window.Telegram?.WebApp?.initDataUnsafe?.user) {
                const tgUser = window.Telegram.WebApp.initDataUnsafe.user;
                tgUserId = tgUser.id;
                setUsername(tgUser.username ? `@${tgUser.username}` : tgUser.first_name);
                window.Telegram.WebApp.expand();
            } else {
                setUsername("Browser User");
            }

            try {
                const res = await axios.post(`${API_URL}/auth/login/${tgUserId}`);
                setUser(res.data);
                setView('menu');
            } catch (e) {
                alert("Ошибка соединения с сервером");
            }
        };
        initApp();
    }, []);


    const loadMyAccess = async () => {
        try {
            const res = await axios.get(`${API_URL}/projects/access/${user.userId}`);
            setMyUnlockedPolls(res.data);
        } catch(e) { console.error(e); }
    };

    const loadMyCreated = async () => {
        try {
            const res = await axios.get(`${API_URL}/projects/my/${user.userId}`);
            setMyCreatedPolls(res.data);

            const newStats = {};
            await Promise.all(res.data.map(async (p) => {
                try {
                    const statsRes = await axios.get(`${API_URL}/history/stats/${p.id}`);
                    newStats[p.id] = statsRes.data;
                } catch (e) {
                    console.warn("Нет статистики для", p.id);
                }
            }));
            setStatsMap(newStats);

        } catch(e) { alert("Ошибка загрузки"); }
    };

    const loadHistory = async () => {
        const res = await axios.get(`${API_URL}/history/${user.userId}`);
        setHistory(res.data);
    };


    const handleCreateSubmit = async () => {
        try {
            const filtered = newOptions.filter(o => o.trim() !== '');
            if (filtered.length < 2) return alert("Минимум 2 варианта!");

            const res = await axios.post(`${API_URL}/projects`, {
                title: newTitle,
                description: newDesc,
                creatorId: user.userId,
                options: filtered
            });
            setCreatedCode(res.data.accessCode);
        } catch (e) { alert("Ошибка создания"); }
    };

    const handleUnlock = async () => {
        try {
            const res = await axios.post(`${API_URL}/projects/unlock?userId=${user.userId}&code=${searchCode.toUpperCase()}`);
            alert(`Опрос "${res.data.title}" добавлен!`);
            setSearchCode('');
            setView('my-access');
            loadMyAccess();
        } catch (e) { alert("Код не найден"); }
    };

    const handleSliderChange = (optionId, val) => {
        const current = votes[optionId] || 0;
        const diff = (val * val) - (current * current);
        const used = Object.values(votes).reduce((sum, v) => sum + v*v, 0);

        if (used + diff <= MAX_BUDGET) {
            setVotes({ ...votes, [optionId]: val });
        }
    };

    const submitVotes = async () => {
        try {
            await axios.post(`${API_URL}/votes/batch`, {
                userId: user.userId,
                pollId: activePoll.id,
                votes: votes
            });
            alert("Голоса записаны в Блокчейн!");
            setView('menu');
            setVotes({});
        } catch (e) {
            alert(e.response?.data || "Ошибка");
        }
    };

    const copyCode = (code) => {
        navigator.clipboard.writeText(code);
        alert("Код скопирован!");
    };


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

    if (view === 'loading') {
        return (
            <Box display="flex" justifyContent="center" alignItems="center" height="100vh">
                <CircularProgress />
            </Box>
        );
    }

    if (view === 'menu') {
        return (
            <Container maxWidth="sm" sx={{ py: 4 }}>
                <Box textAlign="center" mb={4}>
                    <Typography variant="h4" fontWeight="bold" gutterBottom>QV Voting</Typography>
                    <Typography variant="subtitle1" color="text.secondary">Привет, {username}!</Typography>
                    <Chip label={`ID: ${user?.userId}`} variant="outlined" size="small" sx={{mt: 1}}/>
                </Box>

                <Stack spacing={2}>
                    <Paper elevation={3} sx={{ p: 3, borderRadius: 4 }}>
                        <Typography variant="h6" gutterBottom>🔓 Ввести код</Typography>
                        <Box display="flex" gap={1}>
                            <TextField
                                fullWidth size="small"
                                placeholder="Например: A1B2C3"
                                value={searchCode}
                                onChange={e => setSearchCode(e.target.value)}
                                sx={{ bgcolor: '#f5f5f5', borderRadius: 1 }}
                            />
                            <Button variant="contained" onClick={handleUnlock}>OK</Button>
                        </Box>
                    </Paper>

                    <Button
                        variant="contained" size="large"
                        startIcon={<AddCircleOutlineIcon />}
                        onClick={() => { setCreatedCode(null); setView('creator'); }}
                        sx={{ py: 2, borderRadius: 3 }}
                    >
                        Создать Опрос
                    </Button>

                    <Grid container spacing={2}>
                        <Grid item xs={6}>
                            <Button
                                variant="outlined" fullWidth size="large"
                                startIcon={<HowToVoteIcon />}
                                onClick={() => { setView('my-access'); loadMyAccess(); }}
                                sx={{ height: '100%', py: 2, borderRadius: 3 }}
                            >
                                Голосовать
                            </Button>
                        </Grid>
                        <Grid item xs={6}>
                            <Button
                                variant="outlined" fullWidth size="large"
                                startIcon={<AssessmentIcon />}
                                onClick={() => { setView('my-created'); loadMyCreated(); }}
                                sx={{ height: '100%', py: 2, borderRadius: 3 }}
                            >
                                Мои опросы
                            </Button>
                        </Grid>
                    </Grid>

                    <Button
                        startIcon={<HistoryIcon />}
                        onClick={() => { setView('history'); loadHistory(); }}
                        color="secondary"
                    >
                        История транзакций
                    </Button>
                </Stack>
            </Container>
        );
    }

    if (view === 'creator') {
        if (createdCode) {
            return (
                <Container maxWidth="sm" sx={{ py: 4, textAlign: 'center' }}>
                    <Typography variant="h5" color="success.main" gutterBottom>Успешно!</Typography>
                    <Typography color="text.secondary">Отправьте этот код участникам:</Typography>

                    <Card sx={{ my: 4, bgcolor: '#e3f2fd', border: '1px dashed #2196f3' }}>
                        <CardContent>
                            <Typography variant="h2" sx={{ fontWeight: 'bold', letterSpacing: 6, color: '#1565c0' }}>
                                {createdCode}
                            </Typography>
                        </CardContent>
                    </Card>

                    <Button variant="contained" fullWidth onClick={() => setView('menu')}>В главное меню</Button>
                </Container>
            );
        }
        return (
            <Container maxWidth="sm">
                <Header title="Новый опрос" onBack={() => setView('menu')} />
                <Stack spacing={2}>
                    <TextField label="Заголовок" fullWidth value={newTitle} onChange={e => setNewTitle(e.target.value)} />
                    <TextField label="Описание" fullWidth multiline rows={3} value={newDesc} onChange={e => setNewDesc(e.target.value)} />

                    <Typography variant="subtitle2" sx={{ mt: 2 }}>Варианты ответов:</Typography>
                    {newOptions.map((opt, i) => (
                        <TextField
                            key={i} size="small" placeholder={`Вариант ${i+1}`} fullWidth
                            value={opt} onChange={e => {
                            const arr = [...newOptions]; arr[i] = e.target.value; setNewOptions(arr);
                        }}
                        />
                    ))}
                    <Button startIcon={<AddCircleOutlineIcon />} onClick={() => setNewOptions([...newOptions, ''])}>
                        Добавить вариант
                    </Button>

                    <Button variant="contained" size="large" onClick={handleCreateSubmit} disabled={!newTitle} sx={{ mt: 2 }}>
                        Создать
                    </Button>
                </Stack>
            </Container>
        );
    }

    if (view === 'my-access') {
        return (
            <Container maxWidth="sm">
                <Header title="Доступные мне" onBack={() => setView('menu')} />
                {myUnlockedPolls.length === 0 && <Typography align="center" mt={4} color="text.secondary">Нет опросов. Введите код в меню.</Typography>}

                <Stack spacing={2}>
                    {myUnlockedPolls.map(poll => (
                        <Card key={poll.id} elevation={2}>
                            <CardContent>
                                <Typography variant="h6" gutterBottom>{poll.title}</Typography>
                                <Typography variant="body2" color="text.secondary" paragraph>
                                    {poll.description || "Без описания"}
                                </Typography>
                                <Button variant="contained" fullWidth onClick={() => { setActivePoll(poll); setVotes({}); setView('voting'); }}>
                                    Открыть голосование
                                </Button>
                            </CardContent>
                        </Card>
                    ))}
                </Stack>
            </Container>
        );
    }

    if (view === 'voting' && activePoll) {
        const used = Object.values(votes).reduce((sum, v) => sum + v*v, 0);
        const remaining = MAX_BUDGET - used;

        return (
            <Container maxWidth="sm" sx={{ pb: 10 }}>
                <Header title={activePoll.title} onBack={() => setView('my-access')} />

                <Paper elevation={4} sx={{
                    position: 'sticky', top: 10, zIndex: 100, p: 2, mb: 3, borderRadius: 3,
                    bgcolor: remaining < 0 ? '#ffebee' : '#e8f5e9',
                    border: remaining < 0 ? '1px solid red' : '1px solid green'
                }}>
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                        <Typography variant="subtitle1">Бюджет:</Typography>
                        <Typography variant="h5" fontWeight="bold" color={remaining < 0 ? 'error' : 'success.main'}>
                            {remaining} / {MAX_BUDGET} QV
                        </Typography>
                    </Box>
                </Paper>

                <Stack spacing={2}>
                    {activePoll.options.map(opt => {
                        const val = votes[opt.id] || 0;
                        const cost = val * val;
                        return (
                            <Card key={opt.id} variant="outlined">
                                <CardContent>
                                    <Typography variant="subtitle1" fontWeight="500">{opt.text}</Typography>
                                    <Box display="flex" alignItems="center" gap={2} mt={1}>
                                        <Slider
                                            value={val} min={0} max={10} step={1} marks
                                            onChange={(e, v) => handleSliderChange(opt.id, v)}
                                            sx={{ flexGrow: 1 }}
                                        />
                                        <Box textAlign="center" minWidth={40}>
                                            <Typography variant="h6">{val}</Typography>
                                        </Box>
                                    </Box>
                                    <Typography variant="caption" color="text.secondary" display="block" align="right">
                                        Цена: <b>{cost}</b> QV
                                    </Typography>
                                </CardContent>
                            </Card>
                        );
                    })}
                </Stack>

                <Box position="fixed" bottom={0} left={0} right={0} p={2} bgcolor="white" borderTop="1px solid #eee">
                    <Button
                        variant="contained" fullWidth size="large"
                        disabled={used === 0 || remaining < 0}
                        onClick={submitVotes}
                    >
                        Отправить голос
                    </Button>
                </Box>
            </Container>
        );
    }

    if (view === 'my-created') {
        return (
            <Container maxWidth="sm">
                <Header title="Я создал" onBack={() => setView('menu')} />
                {myCreatedPolls.length === 0 && <Typography align="center" mt={4}>Вы еще не создавали опросов</Typography>}

                <Stack spacing={2}>
                    {myCreatedPolls.map(p => {
                        const stat = statsMap[p.id];
                        return (
                            <Card key={p.id}>
                                <CardContent>
                                    <Box display="flex" justifyContent="space-between" alignItems="start">
                                        <Typography variant="h6">{p.title}</Typography>
                                        <Chip
                                            label={p.accessCode}
                                            color="primary"
                                            variant="outlined"
                                            onClick={() => copyCode(p.accessCode)}
                                            icon={<ContentCopyIcon sx={{fontSize: 16}}/>}
                                            clickable
                                        />
                                    </Box>

                                    <Divider sx={{ my: 2 }} />

                                    {stat ? (
                                        <Grid container spacing={2} textAlign="center">
                                            <Grid item xs={6}>
                                                <Box bgcolor="#e3f2fd" p={1} borderRadius={2}>
                                                    <GroupIcon color="primary" />
                                                    <Typography variant="caption" display="block">Участников</Typography>
                                                    <Typography variant="h6">{stat.participants}</Typography>
                                                </Box>
                                            </Grid>
                                            <Grid item xs={6}>
                                                <Box bgcolor="#e0f2f1" p={1} borderRadius={2}>
                                                    <EqualizerIcon color="success" />
                                                    <Typography variant="caption" display="block">Сумма голосов</Typography>
                                                    <Typography variant="h6">{stat.totalVotes}</Typography>
                                                </Box>
                                            </Grid>
                                        </Grid>
                                    ) : (
                                        <Box display="flex" justifyContent="center" p={2}>
                                            <CircularProgress size={24} />
                                        </Box>
                                    )}
                                </CardContent>
                            </Card>
                        );
                    })}
                </Stack>
            </Container>
        );
    }

    if (view === 'history') {
        const grouped = getGroupedHistory();

        return (
            <Container maxWidth="sm" sx={{mt: 2, pb: 5}}>
                <Header title="Мой Блокчейн-лог" onBack={() => setView('menu')} />

                {grouped.length === 0 && <Typography align="center" mt={4}>История пуста</Typography>}

                {grouped.map((group, idx) => (
                    <Paper key={idx} elevation={2} sx={{ mb: 3, overflow: 'hidden' }}>
                        <Box bgcolor="#e3f2fd" p={2} display="flex" justifyContent="space-between" alignItems="center">
                            <Typography variant="subtitle1" fontWeight="bold">
                                {group.title}
                            </Typography>
                            <Chip label={`${group.totalCost} QV`} color="primary" size="small" />
                        </Box>

                        <List dense>
                            {group.items.map((h, i) => (
                                <div key={i}>
                                    <ListItem>
                                        <ListItemText
                                            primary={
                                                <Typography variant="body1">
                                                    {h.optionText || `Вариант #${h.optionId}`}
                                                </Typography>
                                            }
                                            secondary={
                                                <Box mt={0.5}>
                                                    <Typography variant="body2" component="span" color="text.primary">
                                                        <b>{h.voteCount}</b> голосов
                                                    </Typography>
                                                    <br/>
                                                    <Typography variant="caption" sx={{ fontFamily: 'monospace', color: '#999' }}>
                                                        Tx: {h.txHash.substring(0, 10)}...
                                                    </Typography>
                                                </Box>
                                            }
                                        />
                                    </ListItem>
                                    {i < group.items.length - 1 && <Divider component="li" />}
                                </div>
                            ))}
                        </List>
                    </Paper>
                ))}
            </Container>
        );
    }
}

export default App;