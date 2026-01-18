import { useState, useEffect } from 'react';
import axios from 'axios';
import {
    Container, Typography, Card, CardContent, Slider, Button,
    List, ListItem, ListItemText, Divider, Chip, TextField, Box, IconButton, CircularProgress
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import AssessmentIcon from '@mui/icons-material/Assessment';

const API_URL = '/api';

function App() {
    const [view, setView] = useState('loading');
    const [user, setUser] = useState(null);
    const [username, setUsername] = useState('');

    // Состояния
    const [history, setHistory] = useState([]);
    const [myProjects, setMyProjects] = useState([]); // Мои созданные опросы
    const [loadingStats, setLoadingStats] = useState({}); // Кэш статистики {projectId: stats}

    // Создание
    const [newProjectTitle, setNewProjectTitle] = useState('');
    const [newProjectDesc, setNewProjectDesc] = useState('');
    const [createdCode, setCreatedCode] = useState(null);

    // Голосование
    const [searchCode, setSearchCode] = useState('');
    const [foundProject, setFoundProject] = useState(null);
    const [voteCount, setVoteCount] = useState(1);

    // Инициализация
    useEffect(() => {
        const initApp = async () => {
            let tgUserId = 101;
            if (window.Telegram?.WebApp?.initDataUnsafe?.user) {
                const tgUser = window.Telegram.WebApp.initDataUnsafe.user;
                tgUserId = tgUser.id;
                setUsername(tgUser.username ? `@${tgUser.username}` : tgUser.first_name);
            } else {
                setUsername("Test User");
            }

            try {
                const res = await axios.post(`${API_URL}/auth/login/${tgUserId}`);
                setUser(res.data);
                setView('role-select');
            } catch (e) {
                alert("Ошибка авторизации");
            }
        };
        initApp();
    }, []);

    // --- ЗАГРУЗЧИКИ ---

    const loadHistory = async () => {
        const res = await axios.get(`${API_URL}/history/${user.userId}`);
        setHistory(res.data);
    };

    const loadMyProjects = async () => {
        try {
            // 1. Получаем список проектов
            const res = await axios.get(`${API_URL}/projects/my/${user.userId}`);
            setMyProjects(res.data);

            // 2. Для каждого проекта подгружаем статистику асинхронно
            res.data.forEach(async (p) => {
                try {
                    const statsRes = await axios.get(`${API_URL}/history/stats/${p.id}`);
                    setLoadingStats(prev => ({...prev, [p.id]: statsRes.data}));
                } catch (e) {
                    console.error("Нет статистики для", p.id);
                }
            });
        } catch (e) {
            alert("Ошибка загрузки проектов");
        }
    };

    // --- ДЕЙСТВИЯ ---

    const copyToClipboard = (text) => {
        navigator.clipboard.writeText(text);
        alert("Код скопирован!");
    };

    const handleCreateProject = async () => {
        try {
            const res = await axios.post(`${API_URL}/projects`, {
                title: newProjectTitle,
                description: newProjectDesc,
                creatorId: user.userId
            });
            setCreatedCode(res.data.accessCode);
        } catch (e) {
            alert('Ошибка создания');
        }
    };

    const handleSearchProject = async () => {
        try {
            setFoundProject(null);
            const res = await axios.get(`${API_URL}/projects/search/${searchCode}`);
            setFoundProject(res.data);
            setVoteCount(1);
        } catch (e) {
            alert('Опрос не найден');
        }
    };

    const handleVote = async () => {
        try {
            await axios.post(`${API_URL}/votes`, {
                userId: user.userId,
                projectId: foundProject.id,
                voteCount: voteCount
            });
            alert('Голос отправлен!');
            setView('role-select');
            setFoundProject(null);
            setSearchCode('');
        } catch (e) {
            if (e.response && e.response.status === 500) {
                alert('Ошибка: Вы уже голосовали или нет средств!');
            } else {
                alert('Ошибка сети');
            }
        }
    };

    // --- UI ---

    if (view === 'loading') return <Typography align="center" mt={5}>Загрузка...</Typography>;

    // 1. ГЛАВНОЕ МЕНЮ
    if (view === 'role-select') {
        return (
            <Container maxWidth="sm" style={{ marginTop: '30px', textAlign: 'center' }}>
                <Typography variant="h4" gutterBottom>Привет, {username}!</Typography>
                <Chip label={`Баланс: ${user?.balance} QV`} color="primary" variant="outlined" style={{marginBottom: 20}} />

                <Box display="flex" flexDirection="column" gap={2}>
                    <Button variant="contained" size="large" onClick={() => { setCreatedCode(null); setView('creator'); }}>
                        ➕ Создать Новый
                    </Button>
                    <Button variant="contained" color="secondary" size="large" onClick={() => setView('voter')}>
                        🔎 Найти и Голосовать
                    </Button>

                    <Divider style={{margin: '10px 0'}}>МОЁ</Divider>

                    <Button variant="outlined" startIcon={<AssessmentIcon/>} onClick={() => { setView('my-projects'); loadMyProjects(); }}>
                        📂 Мои Опросы
                    </Button>
                    <Button variant="outlined" onClick={() => { setView('history'); loadHistory(); }}>
                        📜 История Голосов
                    </Button>
                </Box>
            </Container>
        );
    }

    // 2. ЭКРАН "МОИ ОПРОСЫ" (НОВОЕ)
    if (view === 'my-projects') {
        return (
            <Container maxWidth="sm" style={{ marginTop: '20px' }}>
                <Button onClick={() => setView('role-select')}>← Меню</Button>
                <Typography variant="h5" mt={2} mb={2}>Созданные мной</Typography>

                {myProjects.length === 0 && <Typography align="center">Список пуст</Typography>}

                <List>
                    {myProjects.map((p) => {
                        const stats = loadingStats[p.id];
                        return (
                            <Card key={p.id} style={{marginBottom: 15, background: '#f9f9f9'}}>
                                <CardContent>
                                    <Typography variant="h6">{p.title}</Typography>
                                    <Typography variant="body2" color="textSecondary" paragraph>{p.description}</Typography>

                                    <Divider style={{margin: '10px 0'}} />

                                    <Box display="flex" justifyContent="space-between" alignItems="center">
                                        <Chip
                                            label={p.accessCode}
                                            color="primary"
                                            onClick={() => copyToClipboard(p.accessCode)}
                                            icon={<ContentCopyIcon style={{fontSize: 16}}/>}
                                            clickable
                                        />

                                        {stats ? (
                                            <div style={{textAlign: 'right'}}>
                                                <Typography variant="caption" display="block">Участников: <b>{stats.participants}</b></Typography>
                                                <Typography variant="caption" display="block">Голосов: <b>{stats.totalVotes}</b></Typography>
                                            </div>
                                        ) : (
                                            <CircularProgress size={20} />
                                        )}
                                    </Box>
                                </CardContent>
                            </Card>
                        );
                    })}
                </List>
            </Container>
        );
    }

    // ... (Экраны CREATOR, VOTER, HISTORY остаются без изменений, скопируй их из прошлого ответа) ...

    // 3. ЭКРАН СОЗДАНИЯ
    if (view === 'creator') {
        if (createdCode) {
            return (
                <Container maxWidth="sm" style={{ marginTop: '30px', textAlign: 'center' }}>
                    <Typography variant="h5" gutterBottom color="success.main">Опрос создан!</Typography>
                    <Typography variant="body1">Код доступа:</Typography>
                    <Card style={{margin: '20px 0', background: '#e8f5e9'}}>
                        <CardContent>
                            <Typography variant="h3" style={{fontWeight: 'bold', letterSpacing: 3}}>{createdCode}</Typography>
                        </CardContent>
                    </Card>
                    <Button variant="contained" fullWidth onClick={() => setView('role-select')}>В Меню</Button>
                </Container>
            );
        }
        return (
            <Container maxWidth="sm" style={{ marginTop: '20px' }}>
                <Button onClick={() => setView('role-select')}>← Назад</Button>
                <Typography variant="h5" mt={2}>Новый Опрос</Typography>
                <TextField fullWidth label="Тема" value={newProjectTitle} onChange={e => setNewProjectTitle(e.target.value)} margin="normal"/>
                <TextField fullWidth label="Описание" value={newProjectDesc} onChange={e => setNewProjectDesc(e.target.value)} margin="normal" multiline rows={3}/>
                <Button variant="contained" fullWidth style={{marginTop: 20}} onClick={handleCreateProject} disabled={!newProjectTitle}>Создать</Button>
            </Container>
        );
    }

    // 4. ЭКРАН ГОЛОСОВАНИЯ
    if (view === 'voter') {
        return (
            <Container maxWidth="sm" style={{ marginTop: '20px' }}>
                <Button onClick={() => setView('role-select')}>← Назад</Button>
                {!foundProject ? (
                    <Box mt={2}>
                        <Typography variant="h6" align="center" gutterBottom>Поиск опроса</Typography>
                        <Box display="flex" gap={1}>
                            <TextField fullWidth label="Код опроса" value={searchCode} onChange={e => setSearchCode(e.target.value.toUpperCase())}/>
                            <Button variant="contained" onClick={handleSearchProject}>Найти</Button>
                        </Box>
                    </Box>
                ) : (
                    <Card style={{marginTop: 20}}>
                        <CardContent>
                            <Typography variant="h5">{foundProject.title}</Typography>
                            <Typography color="textSecondary" paragraph>{foundProject.description}</Typography>
                            <Divider sx={{my: 2}}/>
                            <Typography>Ваш голос: {voteCount}</Typography>
                            <Slider value={voteCount} min={1} max={10} onChange={(e, v) => setVoteCount(v)} valueLabelDisplay="auto" marks />
                            <Typography align="right" color="error" variant="h6">Цена: {voteCount * voteCount} QV</Typography>
                            <Button variant="contained" fullWidth sx={{mt: 2}} onClick={handleVote}>Голосовать</Button>
                            <Button size="small" sx={{mt: 1}} onClick={() => setFoundProject(null)}>Отмена</Button>
                        </CardContent>
                    </Card>
                )}
            </Container>
        );
    }

    // 5. ЭКРАН ИСТОРИИ (Транзакции)
    if (view === 'history') {
        return (
            <Container maxWidth="sm" style={{ marginTop: '20px' }}>
                <Button onClick={() => setView('role-select')}>← Меню</Button>
                <Typography variant="h5" mt={2} mb={2}>Мои голоса</Typography>
                <List>
                    {history.map((h, i) => (
                        <div key={i}>
                            <ListItem>
                                <ListItemText
                                    primary={`Опрос ID: ${h.projectId}`}
                                    secondary={`Отдано голосов: ${h.voteCount} (Cost: ${h.cost})`}
                                />
                                <Chip label="OK" color="success" size="small" />
                            </ListItem>
                            <Divider/>
                        </div>
                    ))}
                </List>
            </Container>
        );
    }
}

export default App;